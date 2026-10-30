package com.uc1.nexopay.ui.login

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.ActivityLoginBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import com.uc1.nexopay.ui.common.UiNotice
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

        var lastBottomInset = -1
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(bars.bottom, keyboard.bottom)
            if (bottom != lastBottomInset) {
                lastBottomInset = bottom
                view.setPadding(bars.left, bars.top, bars.right, bottom)
                if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                    view.doOnNextLayout { revealFocusedField() }
                }
            } else {
                view.setPadding(bars.left, bars.top, bars.right, bottom)
            }
            insets
        }

        listOf(binding.inputAccount, binding.inputPassword).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) binding.scrollLogin.post { revealFocusedField() }
            }
        }
        ViewCompat.requestApplyInsets(binding.root)

        binding.buttonLogin.setOnClickListener { submit() }
        binding.inputAccount.doAfterTextChanged {
            binding.inputLayoutAccount.error = null
            viewModel.clearError()
        }
        binding.inputPassword.doAfterTextChanged {
            binding.inputLayoutPassword.error = null
            viewModel.clearError()
        }

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
                UiNotice.show(binding.root, event.text)
            }
        }
    }

    private fun submit() {
        val account = binding.inputAccount.text?.toString().orEmpty().trim()
        val password = binding.inputPassword.text?.toString().orEmpty()
        binding.inputLayoutAccount.error = when {
            account.isBlank() -> getString(R.string.login_account_required)
            !account.matches(Regex("^[0-9]{8}$")) -> getString(R.string.login_account_invalid)
            else -> null
        }
        binding.inputLayoutPassword.error =
            if (password.isBlank()) getString(R.string.login_password_required) else null
        if (binding.inputLayoutAccount.error != null || binding.inputLayoutPassword.error != null) return
        hideKeyboard()
        viewModel.login(
            accountId = account,
            password = password
        )
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

    private fun revealFocusedField() {
        val fieldLayout = when (currentFocus) {
            binding.inputAccount -> binding.inputLayoutAccount
            binding.inputPassword -> binding.inputLayoutPassword
            else -> return
        }
        fieldLayout.requestRectangleOnScreen(
            Rect(0, 0, fieldLayout.width, fieldLayout.height),
            true
        )
    }
}
