package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetAmountBinding
import com.uc1.nexopay.ui.common.UiUtils

class AmountSheet : BottomSheetDialogFragment() {

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

        binding.textTitle.setText(
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

        binding.textAvailable.isVisible = mode == Mode.WITHDRAW
        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputAmount.doAfterTextChanged {
            binding.textError.isVisible = false
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun submit() {
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> "El monto máximo es S/ 100,000.00."
            mode == Mode.WITHDRAW && cents > balanceCents ->
                getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        if (error != null) {
            binding.textError.text = error
            binding.textError.isVisible = true
            return
        }

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
