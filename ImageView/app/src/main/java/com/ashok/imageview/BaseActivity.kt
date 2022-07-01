package com.ashok.imageview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


open class BaseActivity:AppCompatActivity() {

    // To show Short Toast
    fun shortToast(message:String){
        makeText(this, message, LENGTH_SHORT).show()
    }
    // To show Long Toast
    fun longToast(message:String){
        makeText(this, message, LENGTH_LONG).show()
    }

    fun backAlertPress(title:String,message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Exit") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton("Cancel"){dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    //Denied Permission alert Dialogue
    fun deniedPermission(context: Context?, message: String){

        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(context.getString(R.string.title_permission_alert))
        builder.setMessage("$message\n\nTo allow click 'Open Settings'")

        // If the permission is Denied educate the user for the need of permission
        // and Guide user to allow Permission
        builder.setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        builder.setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        alertDialog.show()

    }


}