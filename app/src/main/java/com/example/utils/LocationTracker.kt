package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.wifi.WifiManager
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

object LocationTracker {

    @SuppressLint("MissingPermission")
    fun getOfflineLocationData(context: Context): LocationResult {
        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        var loc: Location? = null
        
        try {
            // Synchronously wait for current location with high accuracy
            val task = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            loc = Tasks.await(task, 5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Ignore & try last known
        }

        if (loc == null) {
            try {
                val lastKnownTask = client.lastLocation
                loc = Tasks.await(lastKnownTask, 3, TimeUnit.SECONDS)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Native Platform LocationManager Fallback (vital for offline, emulators, and GMS-less devices)
        if (loc == null) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                if (lm != null) {
                    val isGpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                    val isNetworkEnabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                    if (isGpsEnabled) {
                        val gpsLoc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        if (gpsLoc != null) {
                            loc = gpsLoc
                        }
                    }
                    if (loc == null && isNetworkEnabled) {
                        val netLoc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                        if (netLoc != null) {
                            loc = netLoc
                        }
                    }
                }
            } catch (e: Exception) {
                // Safe fallback ignore
            }
        }

        val cellTowerInfo = getCellTowerInfo(context)
        val wifiInfo = getNearbyWifiSSIDs(context)

        return if (loc != null) {
            LocationResult(
                lat = loc.latitude,
                lng = loc.longitude,
                accuracy = loc.accuracy.toInt(),
                cellTower = cellTowerInfo,
                wifiSSIDs = wifiInfo,
                isGPS = true
            )
        } else {
            LocationResult(
                lat = 0.0,
                lng = 0.0,
                accuracy = 0,
                cellTower = cellTowerInfo,
                wifiSSIDs = wifiInfo,
                isGPS = false
            )
        }
    }

    private fun getCellTowerInfo(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager == null) return "نامشخص"
            
            var operator = telephonyManager.networkOperatorName ?: "نامشخص"
            if (operator.equals("IRANCELL", ignoreCase = true) || operator.equals("MTNIrancell", ignoreCase = true)) {
                operator = "ایرانسل"
            } else if (operator.equals("MCI", ignoreCase = true) || operator.equals("TCI", ignoreCase = true) || operator.contains("Hamrah", ignoreCase = true)) {
                operator = "همراه اول"
            } else if (operator.equals("Rightel", ignoreCase = true)) {
                operator = "رایتل"
            }
            
            @SuppressLint("MissingPermission")
            val cellList: List<CellInfo>? = telephonyManager.allCellInfo
            if (cellList.isNullOrEmpty()) {
                return "$operator (بدون دکل)"
            }

            var cellDetails = ""
            for (info in cellList) {
                if (info.isRegistered) {
                    when (info) {
                        is CellInfoGsm -> {
                            val identity = info.cellIdentity
                            cellDetails = "شناسه دکل: ${identity.cid}"
                            break
                        }
                        is CellInfoLte -> {
                            val identity = info.cellIdentity
                            cellDetails = "شناسه دکل نسل چهار: ${identity.ci}"
                            break
                        }
                        is CellInfoWcdma -> {
                            val identity = info.cellIdentity
                            cellDetails = "شناسه دکل نسل سه: ${identity.cid}"
                            break
                        }
                    }
                }
            }

            if (cellDetails.isEmpty()) {
                operator
            } else {
                "$operator - $cellDetails"
            }
        } catch (e: Exception) {
            "نامشخص"
        }
    }

    private fun getNearbyWifiSSIDs(context: Context): List<String> {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager == null) return emptyList()
            
            @SuppressLint("MissingPermission")
            val results = wifiManager.scanResults
            if (results.isNullOrEmpty()) return emptyList()
            
            results.sortedByDescending { it.level }
                .take(3)
                .map { if (it.SSID.isNullOrEmpty()) "[مخفی]" else it.SSID }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class LocationResult(
        val lat: Double,
        val lng: Double,
        val accuracy: Int,
        val cellTower: String,
        val wifiSSIDs: List<String>,
        val isGPS: Boolean
    ) {
        fun toSmsString(): String {
            return if (isGPS && lat != 0.0 && lng != 0.0) {
                val englishLat = String.format(java.util.Locale.US, "%.6f", lat)
                val englishLng = String.format(java.util.Locale.US, "%.6f", lng)
                "📍 ردیابی موفقیت‌آمیز گاردین:\n" +
                "محیط روی نقشه گوگل (کلیک کنید):\n" +
                "https://maps.google.com/?q=$englishLat,$englishLng\n\n" +
                "مختصات: $englishLat , $englishLng\n" +
                "خطای احتمالی: $accuracy متر\n" +
                "سیگنال سلولی: $cellTower"
            } else {
                "⚠️ وضعیت ردیاب گاردین:\n" +
                "سیستم لوکیشن فعال است ولی به دلیل نبود دید ماهواره‌ای مستقیم (محیط مسقف) موقعیت جغرافیایی یافت نشد.\n" +
                "آخرین دکل شبکه: $cellTower\n" +
                "وای‌فای‌های فعال مجاور: ${if (wifiSSIDs.isEmpty()) "عدم دسترسی" else wifiSSIDs.joinToString(", ")}"
            }
        }
    }
}
