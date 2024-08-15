package com.example.studywaveadmin

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studywaveadmin.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ForgotPasswordActivity : AppCompatActivity() {
    private val binding:ActivityForgotPasswordBinding by lazy {
        ActivityForgotPasswordBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var email:String
    private lateinit var loadingDialog: Dialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance().reference

        binding.btnForgotPassword.setOnClickListener {
            email = binding.emailForgotPassword.text.toString().trim()

            if(email.isEmpty()){
                binding.emailForgotPassword.error = "Enter Email"
            }

            else{
                forgotPassword(email)
            }
        }



        loadingDialog =  Dialog(this)
        loadingDialog.setContentView(R.layout.loading_forgotpassword)

        if(loadingDialog.window != null){

            loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog.setCancelable(false)
        }


        binding.backBtn.setOnClickListener {
            finish()
        }
        binding.loginText.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }


        // Add touch listener to root view to hide keyboard on touch
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun forgotPassword(email: String) {
        loadingDialog.show()
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            loadingDialog.dismiss()
            if (task.isSuccessful) {
                Toast.makeText(this, "A password reset link has been sent to your email.", Toast.LENGTH_LONG).show()

                val currentUser = auth.currentUser
                currentUser?.let {
                    updatePasswordResetInfoInDatabase(it.uid)
                }
                finish()
            } else {
                Toast.makeText(this, task.exception.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePasswordResetInfoInDatabase(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        val resetInfo = mapOf(
            "lastPasswordReset" to System.currentTimeMillis()
        )
        userRef.updateChildren(resetInfo).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("DatabaseUpdate", "User password reset info updated successfully")
            } else {
                Log.e("DatabaseUpdate", "Failed to update user password reset info", task.exception)
            }
        }
    }


    // Function to hide the keyboard
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}