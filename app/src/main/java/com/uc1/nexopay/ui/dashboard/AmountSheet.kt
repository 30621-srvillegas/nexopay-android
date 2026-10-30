package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetAmountBinding
import com.uc1.nexopay.ui.common.UiUtils

class AmountSheet : WalletSheet() {

    enum class Mode { DEPOSIT, WITHDRAW }

    private var _binding: SheetAmountBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((Long) -> Unit)? = null

    private val mode: Mode
        get() = Mode.valueOf(requireArguments().getString(ARG_MODE)!!)

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetAmountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.textTitle.setText(
            if (mode == Mode.DEPOSIT) {
                R.string.amount_sheet_deposit_title
            } else {
                R.string.amount_sheet_withdraw_title
            }
        )
        binding.buttonConfirm.setText(
            if (mode == Mode.DEPOSIT) {
                R.string.amount_sheet_confirm_deposit
            } else {
                R.string.amount_sheet_confirm_withdraw
            }
        )

        binding.header.imageIcon.setImageResource(
            if (mode == Mode.DEPOSIT) R.drawable.ic_deposit else R.drawable.ic_withdraw
        )
        binding.header.textDescription.setText(R.string.sheet_amount_description)
        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputAmount.doAfterTextChanged {
            binding.inputLayoutAmount.error = null
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    private fun submit() {
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> getString(R.string.amount_sheet_maximum)
            mode == Mode.WITHDRAW && cents > balanceCents ->
                getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        binding.inputLayoutAmount.error = error
        if (error != null) return

        onConfirm?.invoke(cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_BALANCE = "balance"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(mode: Mode, balanceCents: Long): AmountSheet =
            AmountSheet().apply {
                arguments = bundleOf(
                    ARG_MODE to mode.name,
                    ARG_BALANCE to balanceCents
                )
            }
    }
}
