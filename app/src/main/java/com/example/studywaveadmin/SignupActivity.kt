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
import com.example.studywaveadmin.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class SignupActivity : AppCompatActivity() {
    private val binding: ActivitySignupBinding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }

    private lateinit var googleSigninClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var name:String
    private lateinit var email:String
    private lateinit var password:String
    private lateinit var loadingDialog: Dialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //auth initialization
        auth = Firebase.auth

        //database initialization
        database = Firebase.database.reference


        ////G o o g l e  Sign in
        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("286267772069-q0larg3n45u5qatuj904v94omhudfhml.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSigninClient = GoogleSignIn.getClient(this,googleSignInOption)

        binding.googleButton.setOnClickListener {
            val startIntent = googleSigninClient.signInIntent
            launcher.launch(startIntent)
        }




        loadingDialog =  Dialog(this)
        loadingDialog.setContentView(R.layout.loading_dialog)

        if(loadingDialog.window != null){

            loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog.setCancelable(false)
        }

        // Setup click listener for "Already have an account" button
        binding.alreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Add touch listener to root view to hide keyboard on touch
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            false
        }

        //C  R E A T E    A C C O U N T
        binding.btnSignUp.setOnClickListener {
            name = binding.nameSignup.text.toString()
            email = binding.emailSignup.text.toString().trim()
            password = binding.passwordSignup.text.toString().trim()

            if(name.isBlank()||email.isBlank()||password.isBlank()){
                Toast.makeText(this,"Please Fill All the Details", Toast.LENGTH_SHORT).show()
            }

            else{
                createAccount(email,password)
            }
        }


        // Setup window insets listener for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun createAccount(email: String, password: String) {
        loadingDialog.show()
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener {
                task->
            if(task.isSuccessful){
                saveUserData()
                auth.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Please Verify Your Email", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this,LoginActivity::class.java))
                    finish()
                }
            }
            else{
                loadingDialog.dismiss()
                Toast.makeText(this, "Account Creation Failed", Toast.LENGTH_SHORT).show()
                Log.d("Account","createAccount: Failure",task.exception)
            }
        }
    }

    private fun saveUserData() {
        //Take the data from Edit Text
        name = binding.nameSignup.text.toString()
        email = binding.emailSignup.text.toString().trim()
        password = binding.passwordSignup.text.toString().trim()

        val user = UserData(name,email,password)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        //save user data into Firebase Database
        database.child("admin").child(userId).setValue(user)
    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }



    // Google SignUp
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                loadingDialog.show()
                val account: GoogleSignInAccount = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Retrieve and save user data from Google Account
                        val name = account.displayName ?: "N/A"
                        val email = account.email ?: "N/A"
                        val userId = auth.currentUser?.uid
                        val user = UserData(name, email, "")

                        if (userId != null) {
                            database.child("admin").child(userId).setValue(user).addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Successfully signed in with Google", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    loadingDialog.dismiss()
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to store user data", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                        Log.d("Account1", "createAccount: Failure", task.exception)
                    }
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                Log.d("GoogleSignIn", "Google sign-in failed", task.exception)
            }
        } else {
            loadingDialog.dismiss()
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
        }
    }
}
