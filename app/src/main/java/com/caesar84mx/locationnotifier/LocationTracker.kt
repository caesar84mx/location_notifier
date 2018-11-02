package com.caesar84mx.locationnotifier

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import com.caesar84mx.locationnotifier.Utility.Companion.log
import com.google.android.gms.maps.model.LatLng

@SuppressLint("MissingPermission")
class LocationTracker(
    private val context: Context,
    private val intent: Intent?): LocationListener
{
    private var targetLatLong: LatLng? = null
    private var targetLocation: Location? = null
    private var triggerLocation: Location? = null
    private var radius: Double = 100.0
    private var phoneNumber: String? = null
    private var notificationMessage: String? = null
    private var locationManager: LocationManager? = null
    private var distance: Float? = null

    private var done: Boolean = false

    fun isDone(): Boolean = done

    fun getTargetLatLong(): LatLng? = targetLatLong

    fun getRadius(): Double = radius

    fun getPhoneNumber(): String? = phoneNumber

    fun getNotificationMessage(): String? = notificationMessage

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        retrieveDataFromIntent()
    }

    fun listen() {
        log("Starting listening...")

        val provider = Utility.getProvider(locationManager)
        locationManager?.requestLocationUpdates(provider, 1000, 5f, this)
        Utility.notifyUser(context,"Location Notifier", "Tracking started.")
    }

    private fun verifyProximity(location: Location?) {
        log("Verifying proximity to target...")

        if (!done) {
            log("Current location: long=${location?.longitude}, lat=${location?.latitude}")

            if (location == null || targetLocation == null) {
                return
            }

            distance = location.distanceTo(targetLocation)

            if (distance!! <= radius) {
                triggerLocation = location
                sendMessage()
                stop()
            }
        }
    }

    private fun sendMessage() {
        val message = formatMessage(notificationMessage)

        log("Sending message:\n\"$message\"")
        SmsManager.getDefault().sendTextMessage(
            phoneNumber,
            null,
            message,
            null,
            null
        )

        log("Sent.")
    }

    private fun retrieveDataFromIntent() {
        log("Retrieving data from intent...")

        targetLatLong = this.intent!!.getParcelableExtra(Utility.TARGET_LOCATION_KEY)

        targetLocation = Utility.getLocationObject(locationManager)
        targetLocation?.longitude = targetLatLong!!.longitude
        targetLocation?.latitude = targetLatLong!!.latitude

        radius = intent.getDoubleExtra(Utility.TARGET_RADIUS_KEY, 100.0)
        phoneNumber = intent.getStringExtra(Utility.TARGET_PHONE_NUMBER_KEY)
        notificationMessage = intent.getStringExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY) ?: ""

        log("Retrieved.")
    }

    private fun formatMessage(message: String?): String {
        return message + "\n" +
                "--------------" + "\n" +
                "Lat: ${triggerLocation?.latitude}\n" +
                "Long: ${triggerLocation?.longitude}\n" +
                "Distance: $distance"
    }

    override fun onLocationChanged(location: Location?) {
        verifyProximity(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        log("Status changed. Provider $provider, status $status")
    }

    override fun onProviderEnabled(provider: String?) {
        log("Provider enabled, provider $provider")
    }

    override fun onProviderDisabled(provider: String?) {
        log("Provider disabled, provider $provider")
    }

    private fun stop() {
        log("Stopping tracking...")

        done = true
        locationManager?.removeUpdates(this)
        locationManager = null

        when (context) {
            is Service -> context.stopSelf()
            is Activity -> context.finish()
            is AppCompatActivity -> context.finish()
        }
    }
}