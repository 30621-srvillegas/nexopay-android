package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetTransferBinding
import com.uc1.nexopay.ui.common.UiUtils

class TransferSheet : WalletSheet() {

    private var _binding: SheetTransferBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((String, Long) -> Unit)? = null

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    private val ownAccountId: String
        get() = requireArguments().getString(ARG_OWN_ACCOUNT).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.textTitle.setText(R.string.transfer_sheet_title)
        binding.header.textDescription.setText(R.string.sheet_transfer_description)
        binding.header.imageIcon.setImageResource(R.drawable.ic_transfer)

        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputDestination.doAfterTextChanged {
            binding.inputLayoutDestination.error = null
        }
        binding.inputAmount.doAfterTextChanged {
            binding.inputLayoutAmount.error = null
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    private fun submit() {
        val destination = binding.inputDestination.text?.toString().orEmpty().trim()
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val destinationError = when {
            !destination.matches(Regex("^\\d{8}$")) ->
                "La cuenta destino debe tener exactamente 8 dígitos."
            destination == ownAccountId ->
                getString(R.string.transfer_sheet_same_account)
            else -> null
        }
        val amountError = when {
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> getString(R.string.amount_sheet_maximum)
            cents > balanceCents -> getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        binding.inputLayoutDestination.error = destinationError
        binding.inputLayoutAmount.error = amountError
        if (destinationError != null || amountError != null) return

        onConfirm?.invoke(destination, cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BALANCE = "balance"
        private const val ARG_OWN_ACCOUNT = "own_account"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(balanceCents: Long, ownAccountId: String): TransferSheet =
            TransferSheet().apply {
                arguments = bundleOf(
                    ARG_BALANCE to balanceCents,
                    ARG_OWN_ACCOUNT to ownAccountId
                )
            }
    }
}
