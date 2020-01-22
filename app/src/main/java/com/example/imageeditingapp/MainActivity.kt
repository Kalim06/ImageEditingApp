package com.example.imageeditingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import androidx.databinding.DataBindingUtil
import com.example.imageeditingapp.Utils.Companion.toast
import com.example.imageeditingapp.Utils.Companion.setMargins
import com.example.imageeditingapp.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.text_dialog.view.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        //image pick code
        private const val IMAGE_PICK_CODE = 1000
        //Permission code
        private const val PERMISSION_CODE = 1001
    }

    //Variables
    private lateinit var binding: ActivityMainBinding
    private var bitmap: Bitmap? = null
    private var savedImageUri: Uri? = null
    private var permissionState = false
    private var hasText = false
    private var hasLogo = false

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Data bind
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        //Request Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission()
        }

        //Check Intent Type
        if (intent?.action == Intent.ACTION_SEND) {
            setIntentImage(intent)
        } else if (intent?.action == Intent.ACTION_MAIN) {
            selectImage()
        }

        //Make View Draggable
        binding.mainImage.viewTreeObserver.addOnGlobalLayoutListener {
            binding.userText.setOnTouchListener(
                CustomTouchListener(
                    binding.mainImage.width,
                    binding.mainImage.height
                )
            )

            binding.logo.setOnTouchListener(
                CustomTouchListener(
                    binding.mainImage.width,
                    binding.mainImage.height
                )
            )
        }

        //Add Logo
        binding.addLogo.setOnClickListener {
            binding.logo.visibility = View.VISIBLE
            hasLogo = true
            binding.addLogo.isEnabled = false
        }

        //Add Text
        binding.addText.setOnClickListener {

            val dialogView = LayoutInflater.from(this).inflate(R.layout.text_dialog, null)

            val builder = AlertDialog.Builder(this).setView(dialogView).setTitle("Enter Text")

            val dialog = builder.show()

            dialogView.dialogOkBtn.setOnClickListener {

                val inputText = dialogView.dialogText.text.toString()

                if (inputText.isNotEmpty()) {
                    dialog.dismiss()
                    binding.userText.text = inputText
                    binding.userText.visibility = View.VISIBLE
                    hasText = true
                    binding.addText.isEnabled = false
                } else {
                    toast(this, "Enter text first")
                }
            }

            dialogView.dialogCancelBtn.setOnClickListener {
                dialog.dismiss()
            }

        }
    }

    //Set Image from intent
    private fun setIntentImage(intent: Intent) {
        //Set Visibility
        handleImageVisibility()
        //Get Image Uri
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
            // Update UI to show shared image
            bitmap = Utils.getBitmap(uri, this)!!
            binding.mainImage.setImageBitmap(bitmap)
            savedImageUri = null
        }
    }

    //Select Image from gallery
    private fun selectImage() {
        //Set Visibility
        selectImageVisibility()
        //Fab OnClickListener
        binding.addImage.setOnClickListener {
            if (checkPermissions()) {
                pickImageFromGallery()
            } else {
                toast(this, "Grant Storage Permissions")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermission()
                }
            }
        }
    }

    //Check Permission
    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
            ) {
                permissionState = true
                requestPermission()
            }
        } else {
            permissionState = true
        }

        return permissionState
    }

    //Request Permissions
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        //show popup to request runtime permission
        requestPermissions(permissions, PERMISSION_CODE)
    }

    //Handle requested permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //Permission from popup granted
                    permissionState = true
                } else {
                    //Permission from popup denied
                    toast(this, "Grant Storage Permissions")
                }
            }
        }
    }

    //Intent to pick image from gallery
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    //Activity Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            //Update UI to show selected image
            handleImageVisibility()
            val receivedBitmap = Utils.getBitmap(data?.data, this)!!

            bitmap = Bitmap.createScaledBitmap(
                receivedBitmap,
                binding.mainView.width,
                binding.mainView.height,
                false
            )
            binding.mainImage.setImageBitmap(bitmap)
            savedImageUri = null
        }
    }

    //Save File Method
    private fun saveImage() {
        binding.progressBar.visibility = View.VISIBLE

        if (savedImageUri == null) {
            if (bitmap != null) {
                val uri = saveImageToStorage()
                handleSaveImageVisibility()
                binding.mainImage.setMargins(0, 0, 0, 0)
                binding.mainImage.setImageURI(uri)
                savedImageUri = uri
            } else {
                binding.progressBar.visibility = View.GONE
                toast(this, "Select Image First")
            }
        } else {
            binding.progressBar.visibility = View.GONE
            toast(this, "Image Already Saved")
        }
    }

    //Save Image to storage
    private fun saveImageToStorage(): Uri {

        //create copy of bitmap
        val scaledBitmap =
            Bitmap.createScaledBitmap(
                bitmap!!,
                binding.mainImage.width,
                binding.mainImage.height,
                false
            )

        val myBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)

        //Create Canvas
        val canvas = Canvas(myBitmap)

        //Text
        val typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            resources.getFont(R.font.aldrich)
        } else {
            ResourcesCompat.getFont(this, R.font.aldrich)
        }
        val paint = Paint()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.color = Color.WHITE
        paint.textSize = 55f
        paint.typeface = typeface

        //Logo
        val logoBitmap =
            BitmapFactory.decodeResource(resources, R.drawable.logo).scale(280, 80, false)

        //Check what to draw in canvas
        if (hasLogo && hasText) {
            canvas.drawBitmap(logoBitmap, binding.logo.x, binding.logo.y, Paint())
            canvas.drawText(
                binding.userText.text.toString(),
                binding.userText.x,
                binding.userText.y,
                paint
            )
        } else if (hasLogo) {
            canvas.drawBitmap(logoBitmap, binding.logo.x, binding.logo.y, Paint())
        } else if (hasText) {
            canvas.drawText(
                binding.userText.text.toString(),
                binding.userText.x,
                binding.userText.y,
                paint
            )
        }

        //Save Bitmap
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageUri = applicationContext.contentResolver.insert(collection, values)

        try {

            val stream = this.contentResolver.openOutputStream(imageUri!!)

            stream.use {
                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, it!!)
            }

            stream!!.flush()
            stream.close()

            toast(this, "Image Saved Successfully")
        } catch (e: IOException) {
            toast(this, "Failed To Save Image. Please Try Again")
        }

        values.clear()

        return imageUri!!
    }

    //Share Image with text
    private fun shareImage() {
        if (savedImageUri != null) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hello! Check out this app. It's Great for editing images."
                )
                putExtra(Intent.EXTRA_STREAM, savedImageUri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(sendIntent)
        } else {
            toast(this, "Save Image First")
        }
    }

    //Visibility Methods
    @SuppressLint("RestrictedApi")
    private fun handleImageVisibility() {
        binding.mainImage.visibility = View.VISIBLE
        binding.editingTab.visibility = View.VISIBLE
        binding.addImage.visibility = View.GONE
        binding.infoText.visibility = View.GONE
    }

    @SuppressLint("RestrictedApi")
    private fun selectImageVisibility() {
        binding.addImage.visibility = View.VISIBLE
        binding.infoText.visibility = View.VISIBLE
        binding.mainImage.visibility = View.GONE
        binding.editingTab.visibility = View.GONE
    }

    private fun handleSaveImageVisibility() {
        binding.progressBar.visibility = View.GONE
        binding.userText.visibility = View.GONE
        binding.logo.visibility = View.GONE
        binding.editingTab.visibility = View.GONE
    }

    //Inflate Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    //Menu Items OnClick
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.saveImage -> {
                saveImage()
                return true
            }
            R.id.shareImage -> {
                shareImage()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
