package com.example.utils

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import com.example.receiver.GuardianDeviceAdminReceiver
import java.io.File

object RemoteWipeHelper {
    private const val TAG = "RemoteWipeHelper"

    /**
     * Executes remote wipe.
     * If Device Administrator is granted, triggers genuine Android Master Clear / Factory Reset (dpm.wipeData),
     * ensuring that all user accounts, apps, PINs, and external storage are completely erased.
     * Also performs best-effort local database and media purge.
     */
    fun wipeAllData(context: Context): Boolean {
        Log.w(TAG, "Initiating remote data purge & factory reset...")

        // Step 1: Best-effort local cleanup (contacts, media)
        wipeLocalContent(context)

        // Step 2: Full hardware Factory Reset via DevicePolicyManager
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)

        if (dpm != null && dpm.isAdminActive(adminComponent)) {
            try {
                Log.w(TAG, "Device Admin is active! Executing full factory reset via wipeData...")
                // WIPE_EXTERNAL_STORAGE also wipes external SD card
                dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error executing dpm.wipeData: ${e.message}")
                try {
                    // Fallback to basic wipe if flag fails on certain OEM ROMs
                    dpm.wipeData(0)
                    return true
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback wipeData failed: ${fallbackEx.message}")
                }
            }
        } else {
            Log.w(TAG, "Device Admin is not active; hardware factory reset skipped, local purge executed.")
        }

        return false
    }

    private fun wipeLocalContent(context: Context) {
        val resolver = context.contentResolver
        
        // 1. Wipe Contacts
        try {
            val deletedCount = resolver.delete(ContactsContract.RawContacts.CONTENT_URI, null, null)
            Log.d(TAG, "Wiped $deletedCount raw contacts from provider")
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping contacts: ${e.message}")
        }

        // 2. Wipe SMS Messages cached or stored in system tables (if permissions allow)
        try {
            val uri = android.net.Uri.parse("content://sms")
            val deletedSms = resolver.delete(uri, null, null)
            Log.d(TAG, "Wiped $deletedSms SMS messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping SMS database: ${e.message}")
        }

        // 3. Wipe Images & Videos catalog from MediaStore Content Provider
        try {
            val deletedImages = resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null)
            Log.d(TAG, "Wiped $deletedImages images from MediaStore")
            
            val deletedVideos = resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null)
            Log.d(TAG, "Wiped $deletedVideos videos from MediaStore")
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping MediaStore catalog: ${e.message}")
        }

        // 4. Wipe physical directories (DCIM, Pictures, Movies, Downloads)
        try {
            val targetDirectories = listOf(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            )
            for (directory in targetDirectories) {
                if (directory != null && directory.exists()) {
                    deleteDirRecursive(directory)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recursively cleaning directories: ${e.message}")
        }
    }

    private fun deleteDirRecursive(file: File) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteDirRecursive(child)
                }
            }
        }
        try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file/dir ${file.name}: ${e.message}")
        }
    }
}
