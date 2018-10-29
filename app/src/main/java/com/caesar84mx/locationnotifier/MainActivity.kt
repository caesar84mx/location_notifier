package com.caesar84mx.locationnotifier

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import com.caesar84mx.locationnotifier.Utility.Companion.APP_TAG
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*
import java.text.MessageFormat

private const val REQUEST_CODE_LOCATION_POINT = 1
private const val REQUEST_CODE_PHONE_NUM = 2

const val MY_PERMISSION_REQUEST_CODE_READ_CONTACTS = 1
const val MY_PERMISSION_REQUEST_CODE_SEND_SMS = 2

private const val LOCATION_KEY = "location"
private const val RADIUS_KEY = "radius"
private const val PHONE_NUM_KEY = "phone_num"
private const val MESSAGE_KEY = "message"
private const val SWITCH_VISIBILITY_KEY = "switch_visibility"

class MainActivity : Activity(), View.OnClickListener {
    private var location: LatLng? = null
    private var radius: Double? = null
    private var phoneNum: String? = null
    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        setContentView(R.layout.activity_main)

        btnChooseLocation.setOnClickListener(this)
        btnAddContact.setOnClickListener(this)
        btnSubmit.setOnClickListener(this)
        sTaskSwitch.setOnClickListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Log.d(APP_TAG, "Saving state...")

        outState?.putParcelable(LOCATION_KEY, location)
        outState?.putDouble(RADIUS_KEY, if (radius == null) 0.0 else radius!!)
        outState?.putString(PHONE_NUM_KEY, phoneNum)
        outState?.putString(MESSAGE_KEY, message)
        outState?.putInt(SWITCH_VISIBILITY_KEY, sTaskSwitch.visibility)

        Log.d(APP_TAG, "Saved.")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(APP_TAG, "Retrieving results from other activity...")

        if (data == null) {
            Log.d(APP_TAG, "No data received.")
            return
        }

        when (requestCode) {
            REQUEST_CODE_LOCATION_POINT -> {
                processLocationPointRequestResult(data)
            }

            REQUEST_CODE_PHONE_NUM -> {
                processPhoneImportRequestResult(data)
            }
        }

        Log.d(APP_TAG, "Done.")
    }

    override fun onClick(v: View?) {
        Log.d(APP_TAG, "${this.javaClass.simpleName } - Processing click...")

        when (v?.id) {
            btnChooseLocation.id -> handleChooseLocationButtonClick()

            btnAddContact.id -> handleAddContactButtonClick()

            btnSubmit.id -> handleSubmitButtonClick()

            sTaskSwitch.id -> handleTaskSwitchClick()
        }
    }

    private fun processPhoneImportRequestResult(data: Intent) {
        Log.d(APP_TAG, "Retrieving contact data...")

        val phones = Utility.retrievePhoneNumbersFromUri(data.data, this)
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.dialog_title_choose_phone)
            .setItems(phones.toTypedArray()) { _, which ->
                phoneNum = phones[which]
                edPhoneNumber.setText(phoneNum)
            }
            .create()
            .show()
    }

    private fun processLocationPointRequestResult(data: Intent) {
        Log.d(APP_TAG, "Retrieving location data...")

        location = data.getParcelableExtra(LOCATION_KEY)
        radius = data.getDoubleExtra(RADIUS_KEY, 0.0)

        val text = MessageFormat.format(
            getString(R.string.tv_on_map_coords),
            location?.latitude,
            location?.longitude,
            radius.toString()
        )
        tvCoordinates.text = text
    }

    private fun restoreState(savedInstanceState: Bundle) {
        location = savedInstanceState.getParcelable(LOCATION_KEY)
        radius = savedInstanceState.getDouble(RADIUS_KEY)
        phoneNum = savedInstanceState.getString(PHONE_NUM_KEY)
        message = savedInstanceState.getString(MESSAGE_KEY)
        val visibility = savedInstanceState.getInt(SWITCH_VISIBILITY_KEY)

        val text = MessageFormat.format(
            getString(R.string.tv_on_map_coords),
            location?.latitude,
            location?.longitude,
            radius.toString()
        )
        tvCoordinates.text = text

        edPhoneNumber.setText(phoneNum)
        edShortMessage.setText(message)

        sTaskSwitch.visibility = visibility
    }

    private fun handleTaskSwitchClick() {
        var intent: Intent? = null

        if (sTaskSwitch.isChecked) {
            Utility.checkPermission(Manifest.permission.SEND_SMS, MY_PERMISSION_REQUEST_CODE_SEND_SMS, this)

            intent = Intent(this, LocationTrackingService::class.java)
                .putExtra(Utility.TARGET_LOCATION_KEY, location)
                .putExtra(Utility.TARGET_RADIUS_KEY, radius)
                .putExtra(Utility.TARGET_PHONE_NUMBER_KEY, phoneNum)
                .putExtra(Utility.TARGET_NOTIFICATION_MESSAGE_KEY, message)

            startService(intent)
        } else {
            if (intent != null) {
                stopService(intent)
            }
        }
    }

    private fun handleSubmitButtonClick() {
        message = edShortMessage.text.toString()

        if (location == null || radius == null || phoneNum == null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(R.string.dialog_error_message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_confirm_data)
                .setMessage(
                    MessageFormat.format(
                        getString(R.string.dialog_confirmation_message),
                        location?.latitude,
                        location?.longitude,
                        radius,
                        phoneNum,
                        message
                    )
                )
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Submit") { dialog, _ ->
                    sTaskSwitch.visibility = View.VISIBLE
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun handleAddContactButtonClick() {
        Utility.checkPermission(Manifest.permission.READ_CONTACTS, MY_PERMISSION_REQUEST_CODE_READ_CONTACTS, this)
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PHONE_NUM)
    }

    private fun handleChooseLocationButtonClick() {
        val newIntent = Intent(applicationContext, ChooseLocationOnMapActivity::class.java)
        startActivityForResult(newIntent, REQUEST_CODE_LOCATION_POINT)
    }
}
