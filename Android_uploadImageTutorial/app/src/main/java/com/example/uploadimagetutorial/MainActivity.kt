package com.example.uploadimagetutorial

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import com.example.uploadimagetutorial.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

    private var selectedImageUri: Uri? = null

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.imageView.setOnClickListener {
            openImageChooser()
        }
        binding.buttonUpload.setOnClickListener {
            uploadImage()
        }
    }

    private fun uploadImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
            return
        }
        val parcelFileDescriptor = contentResolver.openFileDescriptor(
            selectedImageUri!!, "r", null
        ) ?: return

        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        binding.progressBar.progress = 0
        val body = UploadRequestBody(file, "image", this)

        MyApi().uploadImage(MultipartBody.Part.createFormData(
            "image",
            file.name,
            body
        ),
            RequestBody.create(MediaType.parse("multipart/form-data"), "json")
        ).enqueue(object : retrofit2.Callback<UploadResponse>{
            override fun onResponse(
                call: retrofit2.Call<UploadResponse>,
                response: Response<UploadResponse>
            ) {
                response.body()?.let {
                    binding.progressBar.progress = 100
                }
            }

            override fun onFailure(call: retrofit2.Call<UploadResponse>, t: Throwable) {
                binding.progressBar.progress = 0
            }

        })
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            val mimeType = arrayOf("image/jpeg", "image/png")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeType)
            startActivityForResult(it, REQUEST_CODE_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Activity.RESULT_OK) {
            when(requestCode) {
                REQUEST_CODE_IMAGE -> {
                    selectedImageUri = data?.data
                    binding.imageView.setImageURI(selectedImageUri)
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE_IMAGE = 101
    }

    override fun onProgressUpdate(percentage: Int) {
        binding.progressBar.progress = percentage
    }
}

private fun ContentResolver.getFileName(selectedImageUri: Uri): String {
    var name = ""
    val returnCursor = this.query(selectedImageUri, null, null, null, null)
    if (returnCursor != null) {
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        name = returnCursor.getString(nameIndex)
        returnCursor.close()
    }
    return name
}
