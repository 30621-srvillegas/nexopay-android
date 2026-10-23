package com.uc1.nexopay.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.ActivityLoginBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import com.uc1.nexopay.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var prefilled = false

    private val viewModel: LoginViewModel by viewModels {
        NexoViewModelFactory((application as NexoPayApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.buttonLogin.setOnClickListener { submit() }
        binding.cardDemo.setOnClickListener { fillDemo() }

        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        observe()
        viewModel.restoreSession()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        val busy = state.isLoading || state.isRestoring

        binding.progressLogin.isVisible = busy
        binding.buttonLogin.isEnabled = !busy
        binding.inputAccount.isEnabled = !busy
        binding.inputPassword.isEnabled = !busy

        if (!prefilled && binding.inputAccount.text.isNullOrBlank() && state.lastAccount.isNotBlank()) {
            binding.inputAccount.setText(state.lastAccount)
            prefilled = true
        }

        binding.textError.isVisible = state.error != null
        binding.textError.text = state.error.orEmpty()
    }

    private fun handleEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.Success -> openDashboard()
            is LoginEvent.Message -> {
                Snackbar.make(binding.root, event.text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun submit() {
        hideKeyboard()
        viewModel.login(
            accountId = binding.inputAccount.text?.toString().orEmpty(),
            password = binding.inputPassword.text?.toString().orEmpty()
        )
    }

    private fun fillDemo() {
        binding.inputAccount.setText("10020001")
        binding.inputPassword.setText("Nexo#2026")
        binding.textError.isVisible = false
    }

    private fun openDashboard() {
        if (isFinishing || isDestroyed) return
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
