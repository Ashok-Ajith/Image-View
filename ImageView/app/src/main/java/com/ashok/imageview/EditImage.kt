package com.ashok.imageview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EditImage : BaseActivity() {
    private lateinit var lytCamera: ConstraintLayout
    private lateinit var lytPreview: ConstraintLayout
    private lateinit var fabTakePicture: FloatingActionButton
    private lateinit var pviewCamera: PreviewView
    private lateinit var imgCaptured: ImageView
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnCrop: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var bitmapList: ArrayList<Bitmap> = ArrayList()
    private var bitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Edit"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_edit_image)
        supportActionBar?.hide()

        // Initializing UI Components
        lytCamera = findViewById(R.id.lyt_camera)
        lytPreview = findViewById(R.id.lyt_preview)
        fabTakePicture = findViewById(R.id.fabTakePicture)
        pviewCamera = findViewById(R.id.pviewCamera)
        imgCaptured = findViewById(R.id.img_captured)
        btnUndo = findViewById(R.id.btn_undo)
        btnCrop = findViewById(R.id.btn_crop)
        btnSave = findViewById(R.id.btn_save)

        startCamera()

        // Button to take picture while camera mode
        fabTakePicture.setOnClickListener {
            takePhoto()
        }

        // Undo rotated or cropped Process of Image
        btnUndo.setOnClickListener {
            val processList =  bitmapList.size
            if (processList > 0){
                bitmap = bitmapList[processList - 1]
                imgCaptured.setImageBitmap(bitmap)
                bitmapList.removeAt(processList - 1)
            }else{
                shortToast("No process found to UNDO ")
            }
        }

        // Crop Selected or Taken image
        btnCrop.setOnClickListener {
            cropImage()
        }


        // Saved Image's Uri send back to HomePage using setResult Intent
        btnSave.setOnClickListener {
            val finalUri = getImageUriFromBitmap()
            val intent = Intent()
            intent.putExtra("DATA", finalUri.toString())
            if (parent == null) {
                setResult(Constants.CAMERA_IMAGE, intent)
                finish()
            } else {
                parent.setResult(101, intent)
                finish()
            }
        }

        outputDirectory = getCacheDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    // Method to convert Bitmap to URI, Bitmap saved to Scoped Storage of app
    // and return Uri of saved Image
    private fun getImageUriFromBitmap(): Uri? {

        val finalFileDir = File(getExternalFilesDir(""),"Images")
        finalFileDir.mkdirs()

        val finalFile = File(finalFileDir, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")
        finalFile.createNewFile()

        val bytes = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fos = FileOutputStream(finalFile)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        return Uri.fromFile(finalFile)
    }

    private fun cropImage() {
        val builder = AlertDialog.Builder(this)
        val customLayout: View = layoutInflater.inflate(R.layout.layout_crop_alert,null)
        builder.setView(customLayout)

        // Initializing UI Components
        val cropImageView = customLayout.findViewById<CropUtils.CropImageView>(R.id.cropimageview)
        val btnSaveCrop = customLayout.findViewById<MaterialButton>(R.id.btn_saveCrop)
        val btnCancelCrop = customLayout.findViewById<MaterialButton>(R.id.btn_cancelCrop)

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)

        cropImageView.setImageBitmap(bitmap)
        // while clicking save on crop dialog the image converted and saved as bitmap
        btnSaveCrop.setOnClickListener {
            try{
                bitmapList.add(bitmap!!)
                bitmap = cropImageView.croppedImage
                imgCaptured.setImageBitmap(bitmap)
            }catch (e: Exception){
                shortToast("Unable to Crop Image")
            }finally {
                alertDialog.dismiss()
            }
        }
        btnCancelCrop.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    // creates a folder inside cache storage
    private fun getCacheDirectory(): File {
        val mediaDir = File(cacheDir, resources.getString(R.string.app_name))
        mediaDir.mkdirs()
        return if (mediaDir.exists())
            mediaDir else filesDir
    }

    // Method called when clicking the capture button
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener,
        // which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    longToast("Unable to take picture. Please try again or after sometime")
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        lytCamera.visibility = View.GONE
                        bitmap = getBitmapFromUri(output.savedUri.toString())
                        imgCaptured.setImageBitmap(bitmap)
                        supportActionBar?.show()
                        lytPreview.visibility = View.VISIBLE
                    } catch (e: Exception) {
                    }

                }
            })
    }



    // Method used to setup camera builder and launch camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview builder
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(pviewCamera.surfaceProvider)
            // Image capture builder
            imageCapture = ImageCapture.Builder().build()

            // Select Front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
            }

        }, ContextCompat.getMainExecutor(this))
    }



    override fun onDestroy() {
        super.onDestroy()
        // clean cache directory because image taken
        // from camera stored temp on cache directory
        try{
            val file = File(cacheDir, resources.getString(R.string.app_name))
            file.delete()
        }catch (e:Exception){

        }finally {
            cameraExecutor.shutdown()
        }

    }

    // To handle back arrow click in toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (bitmap != null){
            // An alert to confirm the user to exit without Saving image
            backAlertPress("Are you sure to exit ?","If you exit without save will lead to loss image")
        }else{
            super.onBackPressed()
        }

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