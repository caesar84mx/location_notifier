package com.caesar84mx.locationnotifier

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import com.caesar84mx.locationnotifier.Utility.Companion.BROADCAST_ACTION
import com.caesar84mx.locationnotifier.Utility.Companion.log

class LocationTrackingService : Service() {
    private var tracker: LocationTracker? = null

    private val binder = LocationServiceTrackerBinder()

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        log("Starting service...")

        tracker = LocationTracker(this, intent)
        tracker?.listen()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()

        log( "Destroying service...")

        if (tracker!!.isDone()) {
            log( "Task completed")
            Utility.notifyUser(
                this,
                getString(R.string.notification_location_notifier_title_text),
                getString(R.string.notification_service_stopped_text)
            )
        } else {
            log( "Task not completed, restarting")

            val broadcastReceiver = ServiceRestarterBroadcastReceiver()
            val intentFilter = IntentFilter(BROADCAST_ACTION)
            val broadcastIntent = Intent(BROADCAST_ACTION)

            broadcastIntent
                .putExtra(Utility.TARGET_LOCATION_KEY, tracker?.getTargetLatLong())
                .putExtra(Utility.TARGET_RADIUS_KEY, tracker?.getRadius())
                .putExtra(Utility.TARGET_PHONE_NUMBER_KEY, tracker?.getPhoneNumber())
                .putExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY, tracker?.getNotificationMessage())

            registerReceiver(broadcastReceiver, intentFilter)
            sendBroadcast(broadcastIntent)
        }
    }

    private inner class LocationServiceTrackerBinder : Binder() {
        internal val service: LocationTrackingService
            get() {
                log( "Getting service: ${this@LocationTrackingService}")
                return this@LocationTrackingService
            }
    }
}
