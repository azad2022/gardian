package com.example.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

object CameraHelper {
    private const val TAG = "CameraHelper"

    @SuppressLint("MissingPermission")
    fun captureIntruderPhoto(context: Context) {
        val dbScope = CoroutineScope(Dispatchers.IO)
        
        // Ensure Camera permission is granted
        val hasCameraPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        if (!hasCameraPermission) {
            Log.e(TAG, "Cannot capture photo: Camera permission not granted")
            dbScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    db.logDao().insertLog(LogEntry(
                        eventType = "ERROR",
                        description = "تلاش برای ثبت تصویر سارق ناموفق بود",
                        details = "دلیل: مجوز سیستم برای دسترسی به دوربین صادر نشده است."
                    ))
                } catch (e: Exception) {
                    // Ignore
                }
            }
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            Log.e(TAG, "CameraManager not available")
            return
        }

        // Run entire capture on a dedicated background thread to prevent blocking main UI thread
        val thread = HandlerThread("IntruderPhotoThread").apply { start() }
        val handler = Handler(thread.looper)

        handler.post {
            try {
                // Find Front Camera
                var frontCameraId: String? = null
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = id
                        break
                    }
                }

                // Fallback to back camera if no front camera available
                val targetCameraId = frontCameraId ?: cameraManager.cameraIdList.firstOrNull()
                if (targetCameraId == null) {
                    Log.e(TAG, "No cameras available")
                    thread.quitSafely()
                    return@post
                }

                // Query supported hardware JPEG resolutions from characteristics
                val characteristics = cameraManager.getCameraCharacteristics(targetCameraId)
                val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = streamConfigMap?.getOutputSizes(ImageFormat.JPEG)

                // Pick the most suitable resolution closest to 640x480 (or first supported)
                val chosenSize = jpegSizes?.minByOrNull { kotlin.math.abs(it.width * it.height - 640 * 480) }
                    ?: jpegSizes?.firstOrNull()
                    ?: android.util.Size(640, 480)

                val width = chosenSize.width
                val height = chosenSize.height
                Log.d(TAG, "Configuring capture session for resolution: ${width}x${height}")

                val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)

                imageReader.setOnImageAvailableListener({ reader ->
                    try {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            image.close()

                            // Save to secure private storage first (guaranteed protection from thief deletion)
                            val privatePath = saveToPrivateStorage(context, bytes)
                            
                            // Best-effort save into public gallery MediaStore
                            val savedUri = saveToGallery(context, bytes)
                            
                            dbScope.launch {
                                try {
                                    val db = AppDatabase.getDatabase(context)
                                    db.logDao().insertLog(LogEntry(
                                        eventType = "ALARM",
                                        description = "تصویر سارق مخفیانه ثبت و ذخیره شد 📸",
                                        details = "رزولوشن: ${width}x${height}\nمسیر اختصاصی: $privatePath\nگالری عمومی: ${savedUri ?: "ذخیره در پوشه امن برنامه"}"
                                    ))
                                } catch (dbEx: Exception) {
                                    // Ignore
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error acquiring/saving image: ${e.message}")
                    } finally {
                        imageReader.close()
                    }
                }, handler)

                cameraManager.openCamera(targetCameraId, object : CameraDevice.StateCallback() {
                    private var isDeviceClosed = false
                    private var activeSession: CameraCaptureSession? = null

                    override fun onOpened(camera: CameraDevice) {
                        try {
                            val surfaces = listOf(imageReader.surface)
                            
                            @Suppress("DEPRECATION")
                            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        activeSession = session
                                        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                                        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                            addTarget(imageReader.surface)
                                            // Ensure silent capture: set flash mode/shutter off
                                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                                            set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                                        }
                                        
                                        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult
                                            ) {
                                                super.onCaptureCompleted(session, request, result)
                                                cleanup()
                                            }

                                            override fun onCaptureFailed(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                failure: CaptureFailure
                                            ) {
                                                super.onCaptureFailed(session, request, failure)
                                                cleanup()
                                            }
                                        }, handler)

                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error configuring capture request: ${e.message}")
                                        cleanup()
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    cleanup()
                                }

                                private fun cleanup() {
                                    if (isDeviceClosed) return
                                    isDeviceClosed = true
                                    try {
                                        activeSession?.close()
                                    } catch (e: Exception) {}
                                    try {
                                        camera.close()
                                    } catch (e: Exception) {}
                                    thread.quitSafely()
                                }
                            }, handler)

                        } catch (e: Exception) {
                            Log.e(TAG, "Error in camera callback onOpened: ${e.message}")
                            try { camera.close() } catch (ex: Exception) {}
                            thread.quitSafely()
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        try { camera.close() } catch (ex: Exception) {}
                        thread.quitSafely()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        try { camera.close() } catch (ex: Exception) {}
                        thread.quitSafely()
                    }
                }, handler)

                // Safety timeout fallback: shutdown after 5 seconds of hanging
                handler.postDelayed({
                    try {
                        imageReader.close()
                    } catch (e: Exception) {}
                    thread.quitSafely()
                }, 5000)

            } catch (e: Exception) {
                Log.e(TAG, "Error in background capture poster: ${e.message}")
                thread.quitSafely()
            }
        }
    }

    private fun saveToPrivateStorage(context: Context, bytes: ByteArray): String? {
        return try {
            val dir = java.io.File(context.filesDir, "intruders").apply { if (!exists()) mkdirs() }
            val file = java.io.File(dir, "Intruder_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to private storage: ${e.message}")
            null
        }
    }

    private fun saveToGallery(context: Context, bytes: ByteArray): String? {
        val resolver = context.contentResolver
        val filename = "Intruder_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Guardian")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        try {
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                outputStream.write(bytes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return imageUri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing image output: ${e.message}")
            return null
        }
    }

    fun getIntruderPhotos(context: Context): List<java.io.File> {
        val dir = java.io.File(context.filesDir, "intruders")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    fun deleteIntruderPhoto(file: java.io.File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
