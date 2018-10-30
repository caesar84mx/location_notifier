package com.caesar84mx.locationnotifier

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.support.v4.app.NotificationCompat
import android.telephony.SmsManager
import android.util.Log
import com.caesar84mx.locationnotifier.Utility.Companion.APP_TAG
import com.caesar84mx.locationnotifier.Utility.Companion.networkProviderAvailable
import com.google.android.gms.maps.model.LatLng
import java.util.*

private const val SPEAKER_ID = "loc_notifier_service_speaker"

class LocationTrackingService : Service(), LocationListener {
    private var latLong: LatLng? = null
    private var targetLocation: Location? = null
    private var triggerLocation: Location? = null
    private var radius: Double = 100.0
    private var phoneNumber: String? = null
    private var notificationMessage: String? = null
    private var locationManager: LocationManager? = null
    private var distance: Float? = null

    private var speaker: TextToSpeech? = null

    var isDone: Boolean = false

    private val binder = LocationServiceTrackerBinder()

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(APP_TAG, "Starting service...")

        speaker = TextToSpeech(applicationContext) {
            speaker?.language = Locale.US
        }

        speaker?.speak(getString(R.string.speaker_text_starting_service), TextToSpeech.QUEUE_FLUSH, null, SPEAKER_ID)
        retrieveDataFromIntent(intent)

        val provider = if (Utility.networkProviderAvailable(locationManager)) {
            LocationManager.NETWORK_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER
        }

        locationManager?.requestLocationUpdates(provider, 1000, 5f, this)
        notifySender("Location Notifier", "Target location: ${targetLocation?.longitude}, ${targetLocation?.latitude}")

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

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
        if (!isDone) {
            Log.d(APP_TAG, "Current location: long=${location?.longitude}, lat=${location?.latitude}")

            if (location == null || targetLocation == null) {
                return
            }

            distance = location.distanceTo(targetLocation)

            Log.d(APP_TAG, "Distance to target: $distance meters")

            if (distance!! <= radius) {
                triggerLocation = location
                sendMessage()
                isDone = true
                stopSelf()
            }
        } else {
            Log.d(APP_TAG, "Service must be finished, but is still running...")
        }
    }

    private fun notifySender(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mBuilder = getNotificationBuilder(notificationManager)

        mBuilder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(getString(R.string.notification_content_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setAutoCancel(true)

        with(notificationManager) {
            notify(1, mBuilder.build())
        }
    }

    private fun getNotificationBuilder(notificationManager: NotificationManager): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "3000"
            val name = "loc_notifier"
            val description = "Chanel to transmit location notifier messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(channelId, name, importance)
            mChannel.description = description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(mChannel)
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        }
    }

    private fun formatMessage(message: String?): String {
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
            null
        )
        speaker?.speak(getString(R.string.speaker_text_message_sent), TextToSpeech.QUEUE_FLUSH, null, SPEAKER_ID)

        Log.d(APP_TAG, "Sent.")
    }

    private fun retrieveDataFromIntent(intent: Intent?) {
        Log.d(APP_TAG, "Retrieving data from intent...")

        latLong = intent!!.getParcelableExtra(Utility.TARGET_LOCATION_KEY)

        targetLocation = if (networkProviderAvailable(locationManager)) {
            Location(NETWORK_PROVIDER)
        } else {
            Location(GPS_PROVIDER)
        }
        targetLocation?.longitude = latLong!!.longitude
        targetLocation?.latitude = latLong!!.latitude

        radius = intent.getDoubleExtra(Utility.TARGET_RADIUS_KEY, 100.0)
        phoneNumber = intent.getStringExtra(Utility.TARGET_PHONE_NUMBER_KEY)
        notificationMessage = intent.getStringExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY) ?: ""

        Log.d(APP_TAG, "Retrieved.")
    }

    override fun onDestroy() {
        Log.d(APP_TAG, "Destroying service...")

        if (isDone) {
            Log.d(APP_TAG, "Task completed")
            notifySender(
                getString(R.string.notification_location_notifier_title_text),
                getString(R.string.notification_service_stopped_text)
            )
            speaker?.speak(getString(R.string.speaker_text_exiting), TextToSpeech.QUEUE_FLUSH, null, SPEAKER_ID)

            if (speaker != null) {
                speaker?.stop()
                speaker?.shutdown()
            } else {
                Log.d(APP_TAG, "Task not completed, restarting")

                val broadcastIntent = Intent("com.caesar84mx.RestartNotifier")
                broadcastIntent
                    .putExtra(Utility.TARGET_LOCATION_KEY, targetLocation)
                    .putExtra(Utility.TARGET_RADIUS_KEY, radius)
                    .putExtra(Utility.TARGET_PHONE_NUMBER_KEY, phoneNumber)
                    .putExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY, notificationMessage)
                sendBroadcast(broadcastIntent)
            }
        }

        super.onDestroy()
    }

    private

    inner class LocationServiceTrackerBinder : Binder() {
        internal val service: LocationTrackingService
            get() {
                Log.d(APP_TAG, "Getting service: ${this@LocationTrackingService}")
                return this@LocationTrackingService
            }
    }
}
