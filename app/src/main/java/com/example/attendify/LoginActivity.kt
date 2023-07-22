package com.example.attendify

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.example.attendify.databinding.ActivityLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private var info: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.setOnClickListener {
            showBiometricPrompt()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            checkDeviceHasBiometric()
        }

        val executor: Executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this,executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.i("finger", "onAuthenticationError")
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i("finger", "onAuthenticationSucceeded")
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    setupAnim()
                    delay(3000)
                    navigate()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.i("finger", "onAuthenticationFailed")
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAnim() {
        binding.animationView.repeatCount = LottieDrawable.INFINITE
        binding.animationView.playAnimation()
    }


    private fun navigate() {
        binding.animationView.pauseAnimation()
        val intent = Intent(this@LoginActivity,MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
            info = "App can authenticate using biometrics."
            binding.login.isEnabled = true

        }
        else if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            Log.e("MY_APP_TAG", "No biometric features available on this device.")
            info = "No biometric features available on this device."
            binding.login.isEnabled = false

        }
        else if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
            info = "Biometric features are currently unavailable."
            binding.login.isEnabled = false

        }
        else if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {

            val enrollLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    showBiometricPrompt()

                } else {

                    Toast.makeText(this, "Enrollment failed", Toast.LENGTH_SHORT).show()
                }
            }

            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }
            binding.login.isEnabled = false

            enrollLauncher.launch(enrollIntent)

        }
        Toast.makeText(this, info, Toast.LENGTH_LONG).show()
    }

    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(createPromptInfo())
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Fingerprint")
            .setSubtitle("Touch the sensor to authenticate")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()
    }
}