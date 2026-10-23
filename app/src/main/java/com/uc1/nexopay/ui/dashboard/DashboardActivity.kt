package com.uc1.nexopay.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.data.remote.SocketStatus
import com.uc1.nexopay.databinding.ActivityDashboardBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import com.uc1.nexopay.ui.common.UiUtils
import com.uc1.nexopay.ui.login.LoginActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val adapter = TransactionAdapter()
    private var lastError: String? = null

    private val viewModel: DashboardViewModel by viewModels {
        NexoViewModelFactory((application as NexoPayApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                bars.left + dp(20),
                bars.top,
                bars.right + dp(20),
                bars.bottom
            )
            insets
        }

        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter

        binding.buttonDeposit.setOnClickListener {
            showAmountSheet(AmountSheet.Mode.DEPOSIT)
        }
        binding.buttonWithdraw.setOnClickListener {
            showAmountSheet(AmountSheet.Mode.WITHDRAW)
        }
        binding.buttonTransfer.setOnClickListener { showTransferSheet() }
        binding.buttonService.setOnClickListener { showServiceSheet() }
        binding.buttonRefresh.setOnClickListener { viewModel.refresh() }
        binding.buttonLogout.setOnClickListener { confirmLogout() }

        observe()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        binding.textGreeting.text = getString(
            R.string.dashboard_greeting,
            state.owner.ifBlank { "Usuario" }
        )
        binding.textAccount.text = getString(
            R.string.dashboard_account,
            state.accountId
        )

        binding.textBalance.text =
            if (state.isLoading && state.balanceCents == 0L) {
                getString(R.string.dashboard_balance_loading)
            } else {
                UiUtils.formatCents(state.balanceCents)
            }

        adapter.submitList(state.transactions)

        binding.progressHistory.isVisible =
            state.isLoading && state.transactions.isEmpty()
        binding.layoutEmpty.isVisible =
            !state.isLoading && state.transactions.isEmpty()
        binding.recyclerTransactions.isVisible = state.transactions.isNotEmpty()

        val enabled = !state.isOperating
        binding.buttonDeposit.isEnabled = enabled
        binding.buttonWithdraw.isEnabled = enabled
        binding.buttonTransfer.isEnabled = enabled
        binding.buttonService.isEnabled = enabled
        binding.buttonRefresh.isEnabled = enabled

        renderStatus(state.socketStatus)

        state.error?.let { message ->
            if (message != lastError) {
                lastError = message
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        } ?: run {
            lastError = null
        }
    }

    private fun renderStatus(status: SocketStatus) {
        val textRes: Int
        val backgroundRes: Int
        val colorRes: Int

        when (status) {
            SocketStatus.CONNECTED -> {
                textRes = R.string.dashboard_status_online
                backgroundRes = R.drawable.bg_chip_online
                colorRes = R.color.nexo_success
            }
            SocketStatus.CONNECTING -> {
                textRes = R.string.dashboard_status_connecting
                backgroundRes = R.drawable.bg_chip_offline
                colorRes = R.color.nexo_warning
            }
            SocketStatus.DISCONNECTED -> {
                textRes = R.string.dashboard_status_offline
                backgroundRes = R.drawable.bg_chip_offline
                colorRes = R.color.nexo_chip_offline_text
            }
        }

        binding.textStatus.setBackgroundResource(backgroundRes)
        binding.textStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.textStatus.text = "● ${getString(textRes)}"
    }

    private fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Message -> {
                Snackbar.make(binding.root, event.text, Snackbar.LENGTH_LONG).show()
            }
            DashboardEvent.SessionExpired -> navigateToLogin()
        }
    }

    private fun showAmountSheet(mode: AmountSheet.Mode) {
        val sheet = AmountSheet.newInstance(mode, viewModel.state.value.balanceCents)
        sheet.onConfirm = { amountCents ->
            if (mode == AmountSheet.Mode.DEPOSIT) {
                viewModel.deposit(amountCents)
            } else {
                viewModel.withdraw(amountCents)
            }
        }
        sheet.show(supportFragmentManager, "amount_sheet")
    }

    private fun showTransferSheet() {
        val state = viewModel.state.value
        val sheet = TransferSheet.newInstance(
            balanceCents = state.balanceCents,
            ownAccountId = state.accountId
        )
        sheet.onConfirm = { destination, amountCents ->
            viewModel.transfer(destination, amountCents)
        }
        sheet.show(supportFragmentManager, "transfer_sheet")
    }

    private fun showServiceSheet() {
        val sheet = ServiceSheet.newInstance(viewModel.state.value.balanceCents)
        sheet.onConfirm = { contract, amountCents ->
            viewModel.payService(contract, amountCents)
        }
        sheet.show(supportFragmentManager, "service_sheet")
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dashboard_logout_confirm)
            .setMessage(R.string.dashboard_logout_body)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.dashboard_logout) { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    private fun navigateToLogin() {
        if (isFinishing || isDestroyed) return
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
