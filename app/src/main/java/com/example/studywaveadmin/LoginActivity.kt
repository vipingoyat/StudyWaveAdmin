package com.example.studywaveadmin

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studywave.model.UserData
import com.example.studywaveadmin.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    private lateinit var email: String
    private lateinit var password: String

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingDialog: Dialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Initialize Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Initialize Loading Dialog
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_dialog)
        loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog.setCancelable(false)

        // Configure Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("286267772069-q0larg3n45u5qatuj904v94omhudfhml.apps.googleusercontent.com")
            .requestEmail()
            .build()

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Check if user is logged in
        if (auth.currentUser != null) {
            // User is logged in, redirect to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        // Handle Google Sign-In button click
        binding.googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        // Handle "Don't have an account" click
        binding.dontHaveAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        // Handle "Forgot Password" click
        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Hide keyboard on touch outside input fields
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false
        }

        // Handle Email and Password Login
        binding.btnSignin.setOnClickListener {
            email = binding.emailLogin.text.toString().trim()
            password = binding.passwordLogin.text.toString().trim()

            if (email.isBlank()) {
                Toast.makeText(this, "Please Enter the Email", Toast.LENGTH_SHORT).show()
            } else if (password.isBlank()) {
                Toast.makeText(this, "Please Enter the Password", Toast.LENGTH_SHORT).show()
            } else {
                verifyUserAccount(email, password)
            }
        }

        // Adjust view insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun verifyUserAccount(email: String, password: String) {
        loadingDialog.show()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            loadingDialog.dismiss()
            if (task.isSuccessful) {
                if (auth.currentUser?.isEmailVerified == true) {
                    Toast.makeText(this, "LogIn Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Your Email ID is not Verified", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please Enter Correct Email and Password.", Toast.LENGTH_SHORT).show()
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

    // Google Sign-In
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account: GoogleSignInAccount = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val name = account.displayName ?: "N/A"
                        val email = account.email ?: "N/A"
                        val userId = auth.currentUser?.uid
                        val user = UserData(name, email, "")

                        if (userId != null) {
                            database.child("admin").child(userId).setValue(user).addOnCompleteListener { dbTask ->
                                loadingDialog.dismiss()
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Successfully signed in with Google", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to store user data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                        Log.e("GoogleSignIn", "Google sign-in failed", authTask.exception)
                    }
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                Log.e("GoogleSignIn", "Google sign-in failed", task.exception)
            }
        } else {
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
        }
    }
}