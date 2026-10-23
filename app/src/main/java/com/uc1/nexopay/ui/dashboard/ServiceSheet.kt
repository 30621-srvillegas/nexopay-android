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
import com.uc1.nexopay.databinding.SheetServiceBinding
import com.uc1.nexopay.ui.common.UiUtils

class ServiceSheet : BottomSheetDialogFragment() {

    private var _binding: SheetServiceBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((String, Long) -> Unit)? = null

    private val balanceCents: Long
        get() = requireArguments().getLong(ARG_BALANCE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        binding.inputContract.doAfterTextChanged {
            binding.textError.isVisible = false
        }
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
        val contract = binding.inputContract.text
            ?.toString()
            .orEmpty()
            .trim()
            .uppercase()
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        val error = when {
            !contract.matches(Regex("^(LUZ|AGUA|NET|GAS)-[A-Z0-9]{3,12}$")) ->
                "Usa un código como LUZ-001, AGUA-002, NET-003 o GAS-004."
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> "El monto máximo es S/ 100,000.00."
            cents > balanceCents -> getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        if (error != null) {
            binding.textError.text = error
            binding.textError.isVisible = true
            return
        }

        onConfirm?.invoke(contract, cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BALANCE = "balance"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(balanceCents: Long): ServiceSheet =
            ServiceSheet().apply {
                arguments = bundleOf(ARG_BALANCE to balanceCents)
            }
    }
}
