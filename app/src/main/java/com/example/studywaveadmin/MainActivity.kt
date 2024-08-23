package com.example.studywaveadmin

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.studywave.model.UserData
import com.example.studywaveadmin.Adapter.CourseAdapter
import com.example.studywaveadmin.databinding.ActivityMainBinding
import com.example.studywaveadmin.model.courseData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {
    private val binding:ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var loadingDialog: Dialog
    private lateinit var courseList:ArrayList<courseData>
    private lateinit var courseAdapter:CourseAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize Loading Dialog
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_dialog)

        if (loadingDialog.window != null) {

            loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog.setCancelable(false)
        }

        retrieveAndDisplayCourseDetails()

        binding.btnUploadCourse.setOnClickListener {
            startActivity(Intent(this,UploadCourseActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    private fun retrieveAndDisplayCourseDetails(){
        loadingDialog.show()
        val courseRef =  database.reference.child("course").orderByChild("postedBy").equalTo(auth.uid)
        courseList = arrayListOf()
        courseRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                courseList.clear()
                for(courseSnapshot in snapshot.children){
                    val courseItem = courseSnapshot.getValue(courseData::class.java)
                    courseItem?.let {
                        courseList.add(it)
                    }
                }
                setAdapter(courseList)
                loadingDialog.dismiss()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity,error.message,Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapter(courseList: java.util.ArrayList<courseData>) {
        courseAdapter = CourseAdapter(this,courseList)
        binding.recyclerView.layoutManager = GridLayoutManager(this,2)
        binding.recyclerView.adapter = courseAdapter
        courseAdapter.notifyDataSetChanged()
    }
}