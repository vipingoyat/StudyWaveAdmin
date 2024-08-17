package com.example.studywaveadmin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studywaveadmin.Adapter.PlayListAdapter
import com.example.studywaveadmin.databinding.ActivityUploadPlayListBinding
import com.example.studywaveadmin.model.PlayListModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class UploadPlayListActivity : AppCompatActivity() {
    private val binding: ActivityUploadPlayListBinding by lazy {
        ActivityUploadPlayListBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var videoUri: Uri? = null
    private lateinit var loadingDialog: Dialog

    private lateinit var playList:ArrayList<PlayListModel>

    private lateinit var postID:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
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
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        postID = intent.getStringExtra("postId").toString()

        binding.cardVideo.setOnClickListener {
            pickVideo.launch("video/*")
        }
        retrievePlaylist()

        binding.uploadPlaylist.setOnClickListener {
            val title = binding.title.text.toString()
            if(title.isEmpty()){
                Toast.makeText(this,"Please give any Title to the Video",Toast.LENGTH_SHORT).show()
            }
            else if(videoUri==null){
                Toast.makeText(this,"Please upload Video.",Toast.LENGTH_SHORT).show()
            }
            else{
                uploadPlayList(title,videoUri!!)
            }
        }
    }

    private fun retrievePlaylist() {
        playList = arrayListOf()
        val databaseRef = database.reference.child("course").child(postID).child("playlist")
        databaseRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(list in snapshot.children){
                    val listData = list.getValue(PlayListModel::class.java)
                    listData?.let {
                        playList.add(it)
                    }
                }
                setAdapter(playList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UploadPlayListActivity, "Failed to retrieve playlist: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun setAdapter(playList: java.util.ArrayList<PlayListModel>) {
        val adapter = PlayListAdapter(playList)
        binding.rvPlayList.layoutManager = LinearLayoutManager(this)
        binding.rvPlayList.adapter = adapter
        adapter.notifyDataSetChanged() // Notify adapter of data change
    }

    // Inside your pickVideo ActivityResult
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoUri = uri
            binding.courseVideo.setVideoURI(uri)
            binding.courseVideo.start()
            binding.courseVideoImage.visibility = View.GONE
        } else {
            Toast.makeText(this, "Failed to pick video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPlayList(
        title: String,
        videoUri: Uri
    ) {
        loadingDialog.show()
        val childKey = System.currentTimeMillis().toString()
        val videoStorageRef = storage.reference.child("play_list/$childKey-video")
        // Upload Video
        videoStorageRef.putFile(videoUri).addOnSuccessListener {
            videoStorageRef.downloadUrl.addOnSuccessListener { videoUri ->
                val model = auth.uid?.let { it1 ->
                    PlayListModel(
                        title = title,
                        video = videoUri.toString(),
                        enable = "false"
                    )
                }
                database.reference.child("course").child(postID).child("playlist")
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
}