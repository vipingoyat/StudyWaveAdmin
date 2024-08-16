package com.example.studywaveadmin.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studywave.model.UserData
import com.example.studywaveadmin.databinding.RvCourseDesignBinding
import com.example.studywaveadmin.model.courseData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CourseAdapter(
    private val context: Context,
    private val courseList:ArrayList<courseData>
):RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {
    val database = FirebaseDatabase.getInstance()
    val auth = FirebaseAuth.getInstance()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = RvCourseDesignBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CourseViewHolder(binding)
    }

    override fun getItemCount(): Int = courseList.size

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class CourseViewHolder(private val binding: RvCourseDesignBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val courseD = courseList[position]
            binding.apply {
                courseTitle.text = courseD.title
                price.text = courseD.price.toString()
                val uriString = courseD.thumbnail
                val uri = Uri.parse(uriString)
                Glide.with(context).load(uri).into(courseImage)

                database.reference.child("course").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (courseSnapshot in snapshot.children) {
                            // Retrieve the postedBy UID from the course data
                            val courseData = courseSnapshot.getValue(courseData::class.java)
                            val postedByUid = courseData?.postedBy

                            if (postedByUid != null) {
                                // Use the UID to fetch the user's name and profile from the "user" node
                                database.reference.child("user").child(postedByUid)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(userSnapshot: DataSnapshot) {
                                            val user = userSnapshot.getValue(UserData::class.java)
                                            val uriString = user?.profile
                                            val uri = Uri.parse(uriString)

                                            // Update UI elements
                                            Glide.with(context).load(uri).into(postedByProfile)
                                            binding.name.text = user?.name
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load course data", Toast.LENGTH_SHORT).show()
                    }
                })

            }
        }

    }
}