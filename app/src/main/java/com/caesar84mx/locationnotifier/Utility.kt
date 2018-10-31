package com.caesar84mx.locationnotifier

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat

class Utility private constructor() {
    companion object {
        const val TARGET_LOCATION_KEY = "target_location"
        const val TARGET_RADIUS_KEY = "target_radius"
        const val TARGET_PHONE_NUMBER_KEY = "target_phone_number"
        const val TARGET_NOTIFICATION_MESSAGE_KEY = "target_notification_message"

        const val SPEAKER_ID = "loc_notifier_service_speaker"
        const val BROADCAST_ACTION = "com.caesar84mx.RestartNotifier"

        const val APP_TAG = "loc_notifier"

        @JvmStatic
        fun checkPermission(permission: String, requestCode: Int, context: Context) {
            log( "Checking permission: $permission")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                log( "Has no permission granted. Requesting...")

                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(permission),
                    requestCode
                )

                log( "Done.")
            }

            log( "Checked.")
        }

        @JvmStatic
        fun notifyUser(context: Context, title: String, message: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mBuilder = Utility.getNotificationBuilder(context, notificationManager)

            mBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_content_text))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setAutoCancel(true)

            with(notificationManager) {
                notify(1, mBuilder.build())
            }
        }

        @JvmStatic
        fun retrievePhoneNumbersFromUri(contactUri: Uri?, context: Context?): List<String> {
            log( "Retrieving phone number...")

            val result = mutableListOf<String>()

            val cursorId = context?.contentResolver?.query(
                contactUri!!,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null, null
            )

            // Getting contacts ID
            val contactId = if (cursorId!!.moveToFirst()) {
                cursorId.getString(cursorId.getColumnIndex(ContactsContract.Contacts._ID))
            } else {
                "null"
            }

            cursorId.close()

            //Getting cursor ID
            val cursor = context.contentResolver?.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId), null
            )

            //Getting number
            if (cursor!!.moveToFirst()) {
                do {
                    val num =
                        formatPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                    result.add(num)
                    log( "Retrieving number: $num")
                } while (cursor.moveToNext())
            }

            cursor.close()
            log( "Phone number retrieved.")
            return result
        }

        @JvmStatic
        fun networkProviderAvailable(locationManager: LocationManager?): Boolean {
            log( "Checking network provider availability")
            val result = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            log( if (result) "Enabled" else "Disabled")
            return result
        }

        @JvmStatic
        fun getProvider(locationManager: LocationManager?): String {
            return if (Utility.networkProviderAvailable(locationManager)) {
                LocationManager.NETWORK_PROVIDER
            } else {
                LocationManager.GPS_PROVIDER
            }
        }

        @JvmStatic
        fun getLocationObject(locationManager: LocationManager?): Location {
            return if (Utility.networkProviderAvailable(locationManager)) {
                Location(LocationManager.NETWORK_PROVIDER)
            } else {
                Location(LocationManager.GPS_PROVIDER)
            }
        }

        @JvmStatic
        fun getNotificationBuilder(
            context: Context,
            notificationManager: NotificationManager
        ): NotificationCompat.Builder {
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
                NotificationCompat.Builder(context, channelId)
            } else {
                NotificationCompat.Builder(context)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }
        
        @JvmStatic
        fun log(message: String) {
            log( message)
        }

        private fun formatPhoneNumber(phoneNumber: String?): String {
            return phoneNumber!!
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "")
                .replace("-", "")
        }
    }
}