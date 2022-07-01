package com.ashok.imageview


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.ashok.imageview.Constants.CAMERA_IMAGE
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import java.io.IOException


class MainActivity : BaseActivity() {

    private lateinit var lytImgContainer: ShimmerFrameLayout
    private lateinit var imgPreview: ShapeableImageView
    private lateinit var txtEmptyImageHint: MaterialTextView
    private lateinit var txtFilePath: MaterialTextView
    private lateinit var btnCapture: MaterialButton


    // To get Single permission
    private var requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){ granted->
        if(granted) {
            openCamera()
        } else {
            deniedPermission(this, "Need camera permission to Take Picture")
        }
    }

    // To get Single permission
    private var requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){ granted->
        if(granted) {
            openGallery()
        } else {
            deniedPermission(this, "Need storage permission to pick image from gallery")
        }
    }


    private var imageCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == CAMERA_IMAGE) {
            if (data!!.hasExtra("DATA")) {
                if (result.data != null) {
                    displayImage(result)
                }
            }
        }
    }

    private var imagePicker = registerForActivityResult<String, Uri>(ActivityResultContracts.GetContent()) { uri ->
        if (uri.toString() != "" || uri.toString() != "" )
            imagepickerResult(uri)
        else
            shortToast("Image not selected")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.title = "Home"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initializing UI Components
        lytImgContainer = findViewById(R.id.lyt_imgContainer)
        imgPreview = findViewById(R.id.img_preview)
        txtEmptyImageHint = findViewById(R.id.txt_emptyImageHint)
        txtFilePath = findViewById(R.id.txt_filePath)
        btnCapture = findViewById(R.id.btn_capture)

        lytImgContainer.startShimmer()
        // First checking camera Permission to access and take picture
        // If Permission allowed Open camera else explain the need of permission to user
        btnCapture.setOnClickListener {
            val cameraPermission = Manifest.permission.CAMERA
            if (ActivityCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission.launch(cameraPermission)
            }else{
                openCamera()
            }

        }
    }
    private fun imagepickerResult(uri: Uri?) {

        try {
            val intent = Intent(this,EditImage::class.java)
            intent.putExtra("URI",uri.toString())
            imageCapture.launch(intent)
        } catch (e: IOException) {
            Log.e("tag", e.toString())
        }

    }

    private fun openGallery() {
        imagePicker.launch("image/*")
    }


    private fun displayImage(result: ActivityResult) {

        val data: Intent? = result.data
        if (data!!.hasExtra("DATA")) {
            val uri = data.getStringExtra("DATA")
            txtFilePath.text = uri.toString()
            val bitmap = getBitmapFromUri(uri.toString())
            // There are no request codes
            if (uri != "" && uri != null) {
                imgPreview.setImageBitmap(bitmap)
                txtEmptyImageHint.visibility = View.GONE
                lytImgContainer.stopShimmer()
                lytImgContainer.visibility = View.GONE
            } else {
                Toast.makeText(this, "unable to load image", Toast.LENGTH_SHORT).show()
            }
        }else{
            shortToast("No Data Found")
        }

    }


    // Method to Open camera
    private fun openCamera() {
        val intent = Intent(this, EditImage::class.java)
        imageCapture.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // UnRegister Permission callback if the Permission is denied
        requestCameraPermission.unregister()
        requestStoragePermission.unregister()
        imageCapture.unregister()
        imagePicker.unregister()

    }
    fun getBitmapFromUri( uri:String): Bitmap {
        return  if(Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(
                contentResolver, Uri.parse(uri)
            )
        } else {
            val source = ImageDecoder.createSource(contentResolver,  Uri.parse(uri))
            ImageDecoder.decodeBitmap(source)
        }
    }




}