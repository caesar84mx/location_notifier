package com.caesar84mx.locationnotifier

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import com.caesar84mx.locationnotifier.Utility.Companion.APP_TAG
import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import kotlin.math.sqrt

class LocationTrackingService: Service(), LocationListener {
    private var targetLocation: LatLng? = null
    private var triggerLocation: Location? = null
    private var radius: Double = 100.0
    private var phoneNumber: String? = null
    private var notificationMessage: String? = null
    private var locationManager: LocationManager? = null
    private var distance: Double? = null

    private val binder = LocationServiceTrackerBinder()

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        retrieveDataFromIntent(intent)

        val provider = if (Utility.networkProviderAvailable(locationManager)) {
            LocationManager.NETWORK_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER
        }

        Utility.checkPermission(Manifest.permission.SEND_SMS, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION, this)
        Utility.checkPermission(Manifest.permission.SEND_SMS, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION, this)
        locationManager?.requestLocationUpdates(provider, 1000, 2f, this)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onLocationChanged(location: Location?) {
        verifyProximity(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    private fun verifyProximity(location: Location?) {
        Log.d(APP_TAG, "Current location: long=${location?.longitude}, lat=${location?.latitude}")

        if (location == null || targetLocation == null) {
            return
        }

        distance = sqrt((location.longitude - targetLocation!!.longitude).pow(2) + (location.latitude - targetLocation!!.latitude).pow(2))
        Log.d(APP_TAG, "Distance to target: $distance")

        if (distance!! <= radius) {
            triggerLocation = location
            sendMessage()
            onDestroy()
        }
    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    private fun formatMessage(message: String?):String {
        return message + "\n" +
                "--------------" + "\n" +
                "Lat: ${triggerLocation?.latitude}\n" +
                "Long: ${triggerLocation?.longitude}\n" +
                "Distance: ${distance?.format(3)}"
    }

    private fun sendMessage() {
        val message = formatMessage(notificationMessage)

        Log.d(APP_TAG, "Sending message:\n\"$message\"")
        SmsManager.getDefault().sendTextMessage(
            phoneNumber, null,
            message,
            null, null)

        Log.d(APP_TAG, "Sent.")
    }

    private fun retrieveDataFromIntent(intent: Intent?) {
        targetLocation = intent!!.getParcelableExtra(Utility.TARGET_LOCATION_KEY)
        radius = intent.getDoubleExtra(Utility.TARGET_RADIUS_KEY, 100.0)
        phoneNumber = intent.getStringExtra(Utility.TARGET_PHONE_NUMBER_KEY)
        notificationMessage = intent.getStringExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY)

        if (notificationMessage == null) {
            notificationMessage = ""
        }
    }

    inner class LocationServiceTrackerBinder:Binder() {
        internal val service: LocationTrackingService
            get() {
                return this@LocationTrackingService
            }
    }
}
