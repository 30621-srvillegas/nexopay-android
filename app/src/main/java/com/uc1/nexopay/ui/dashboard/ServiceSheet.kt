package com.uc1.nexopay.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.SheetServiceBinding
import com.uc1.nexopay.ui.common.UiUtils
import com.uc1.nexopay.ui.common.ServiceContract

class ServiceSheet : WalletSheet() {

    private var _binding: SheetServiceBinding? = null
    private val binding get() = _binding!!

    var onConfirm: ((String, Long) -> Unit)? = null
    private var selectedService = -1
    private val services = listOf(
        R.string.service_luz to "LUZ",
        R.string.service_agua to "AGUA",
        R.string.service_internet to "NET",
        R.string.service_gas to "GAS"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedService = savedInstanceState?.getInt(STATE_SERVICE, -1) ?: -1
    }

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

        binding.header.textTitle.setText(R.string.service_sheet_title)
        binding.header.textDescription.setText(R.string.sheet_service_description)
        binding.header.imageIcon.setImageResource(R.drawable.ic_service)

        binding.textAvailable.text = getString(
            R.string.amount_sheet_available,
            UiUtils.formatCents(balanceCents)
        )

        val serviceNames = services.map { getString(it.first) }.toTypedArray()
        binding.inputService.setSimpleItems(serviceNames)
        if (selectedService in services.indices) {
            binding.inputService.setText(serviceNames[selectedService], false)
        }
        binding.inputService.setOnItemClickListener { _, _, position, _ ->
            selectedService = position
            binding.inputLayoutService.error = null
        }
        binding.inputContract.doAfterTextChanged {
            binding.inputLayoutContract.error = null
        }
        binding.inputAmount.doAfterTextChanged {
            binding.inputLayoutAmount.error = null
        }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirm.setOnClickListener { submit() }
    }

    private fun submit() {
        val prefix = services.getOrNull(selectedService)?.second
        val contract = ServiceContract.build(
            prefix,
            binding.inputContract.text?.toString().orEmpty()
        )
        val cents = UiUtils.parseAmountToCents(
            binding.inputAmount.text?.toString().orEmpty()
        )

        binding.inputLayoutService.error =
            if (prefix == null) getString(R.string.service_sheet_select) else null
        binding.inputLayoutContract.error =
            if (prefix != null && contract == null) getString(R.string.service_sheet_contract_invalid) else null
        val amountError = when {
            cents == null -> getString(R.string.amount_sheet_invalid)
            cents > MAX_AMOUNT_CENTS -> getString(R.string.amount_sheet_maximum)
            cents > balanceCents -> getString(R.string.amount_sheet_insufficient)
            else -> null
        }

        binding.inputLayoutAmount.error = amountError
        if (contract == null || amountError != null) return

        onConfirm?.invoke(contract, cents!!)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SERVICE, selectedService)
    }

    companion object {
        private const val ARG_BALANCE = "balance"
        private const val STATE_SERVICE = "selected_service"
        private const val MAX_AMOUNT_CENTS = 10_000_000L

        fun newInstance(balanceCents: Long): ServiceSheet =
            ServiceSheet().apply {
                arguments = bundleOf(ARG_BALANCE to balanceCents)
            }
    }
}
