package com.example.studywaveadmin

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studywaveadmin.databinding.ActivityUploadCourseBinding
import com.example.studywaveadmin.model.courseData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class UploadCourseActivity : AppCompatActivity() {
    private val binding: ActivityUploadCourseBinding by lazy {
        ActivityUploadCourseBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private var introVideoUri: Uri? = null
    private lateinit var loadingDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize Loading Dialog
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.uploading_dialog)

        if (loadingDialog.window != null) {

            loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog.setCancelable(false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.cardThumbnail.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.cardVideo.setOnClickListener {
            pickVideo.launch("video/*")
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.uploadBtn.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val description = binding.descriptionEditText.text.toString()
            val duration = binding.durationEditText.text.toString()
            val price = binding.priceEditText.text.toString()

            if (imageUri == null) {
                Toast.makeText(this, "Please select the Thumbnail image", Toast.LENGTH_SHORT).show()
            } else if (title.isEmpty()) {
                Toast.makeText(this, "Enter the Title", Toast.LENGTH_SHORT).show()
            } else if (description.isEmpty()) {
                Toast.makeText(this, "Enter the description", Toast.LENGTH_SHORT).show()
            } else if (duration.isEmpty()) {
                Toast.makeText(this, "Enter the duration", Toast.LENGTH_SHORT).show()
            } else if (price.isEmpty()) {
                Toast.makeText(this, "Enter the price", Toast.LENGTH_SHORT).show()
            } else {
                uploadCourse(title, description, duration, price, imageUri!!, introVideoUri!!)
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.imgThumbnail.setImageURI(uri)
            imageUri = uri
        }
    }

    // Inside your pickVideo ActivityResult
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            introVideoUri = uri
            binding.courseVideo.setVideoURI(uri)
            binding.courseVideo.start()
            binding.courseVideoImage.visibility = View.GONE
        } else {
            Toast.makeText(this, "Failed to pick video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadCourse(
        title: String,
        description: String,
        duration: String,
        price: String,
        imageUri: Uri,
        introVideoUri: Uri
    ) {
        loadingDialog.show()
        val childKey = System.currentTimeMillis().toString()

        val imageStorageRef = storage.reference.child("thumbnails/$childKey-thumbnail")
        val videoStorageRef = storage.reference.child("videos/$childKey-video")

        // Upload Image
        imageStorageRef.putFile(imageUri).addOnSuccessListener {
            imageStorageRef.downloadUrl.addOnSuccessListener { imageUrl->
                // Upload Video
                videoStorageRef.putFile(introVideoUri).addOnSuccessListener {
                    videoStorageRef.downloadUrl.addOnSuccessListener { videoUri ->
                        val model = auth.uid?.let { it1 ->
                            courseData(
                                title = title,
                                description = description,
                                duration = duration,
                                price = price.toLong(),
                                thumbnail = imageUrl.toString(),
                                introVideo = videoUri.toString(),
                                postedBy = it1,
                                enable = "false"
                            )
                        }
                        database.reference.child("course")
                            .push()
                            .setValue(model).addOnSuccessListener {
                                loadingDialog.dismiss()
                                Toast.makeText(this, "Course Uploaded", Toast.LENGTH_SHORT).show()
                                onBackPressed()
                            }
                    }.addOnFailureListener {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Failed to get video URL", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Failed to upload video", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            loadingDialog.dismiss()
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }
}

class FullScreenVideoView(context: Context, attrs: AttributeSet) : VideoView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

