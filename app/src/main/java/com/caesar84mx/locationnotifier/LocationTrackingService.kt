package com.caesar84mx.locationnotifier

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import com.caesar84mx.locationnotifier.Utility.Companion.APP_TAG
import com.google.android.gms.maps.model.LatLng

class LocationTrackingService: Service(), LocationListener {
    private var targetLocation: Location? = null
    private var triggerLocation: Location? = null
    private var radius: Double = 100.0
    private var phoneNumber: String? = null
    private var notificationMessage: String? = null
    private var locationManager: LocationManager? = null
    private var distance: Float? = null

    private val binder = LocationServiceTrackerBinder()

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(APP_TAG, "Starting service...")

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
        Log.d(APP_TAG, "Binding... Intent: $intent")
        return binder
    }

    override fun onLocationChanged(location: Location?) {
        verifyProximity(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(APP_TAG, "Status changed: $status, provider: $provider")
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

        distance = location.distanceTo(targetLocation)

        Log.d(APP_TAG, "Distance to target: $distance meters")

        if (distance!! <= radius) {
            triggerLocation = location
            sendMessage()
            stopSelf()
        }
    }

    private fun formatMessage(message: String?):String {
        return message + "\n" +
                "--------------" + "\n" +
                "Lat: ${triggerLocation?.latitude}\n" +
                "Long: ${triggerLocation?.longitude}\n" +
                "Distance: $distance"
    }

    private fun sendMessage() {
        val message = formatMessage(notificationMessage)

        Log.d(APP_TAG, "Sending message:\n\"$message\"")
        SmsManager.getDefault().sendTextMessage(
            phoneNumber,
            null,
            message,
            null,
            null)

        Log.d(APP_TAG, "Sent.")
    }

    private fun retrieveDataFromIntent(intent: Intent?) {
        Log.d(APP_TAG, "Retrieving data from intent...")

        val latLong: LatLng? = intent!!.getParcelableExtra(Utility.TARGET_LOCATION_KEY)

        targetLocation = Location(GPS_PROVIDER)
        targetLocation?.longitude = latLong!!.longitude
        targetLocation?.latitude = latLong.latitude

        radius = intent.getDoubleExtra(Utility.TARGET_RADIUS_KEY, 100.0)
        phoneNumber = intent.getStringExtra(Utility.TARGET_PHONE_NUMBER_KEY)
        notificationMessage = intent.getStringExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY)

        if (notificationMessage == null) {
            notificationMessage = ""
        }

        Log.d(APP_TAG, "Retrieved.\n" +
                "Target location: lat=${targetLocation?.latitude}, lng=${targetLocation?.longitude}\n" +
                "Radius: $radius meters\n" +
                "Phone number: $phoneNumber\n" +
                "Message: \"$notificationMessage\"")
    }

    override fun onDestroy() {
        Log.d(APP_TAG, "Destroying service...")
        super.onDestroy()
    }

    inner class LocationServiceTrackerBinder:Binder() {
        internal val service: LocationTrackingService
            get() {
                Log.d(APP_TAG, "Getting service: ${this@LocationTrackingService}")
                return this@LocationTrackingService
            }
    }
}
