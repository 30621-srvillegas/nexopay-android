package com.uc1.nexopay.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.uc1.nexopay.NexoPayApp
import com.uc1.nexopay.R
import com.uc1.nexopay.databinding.FragmentHistoryBinding
import com.uc1.nexopay.ui.common.NexoViewModelFactory
import kotlinx.coroutines.launch

/** Bandeja de pantalla completa que comparte datos y conexión en tiempo real con el inicio. */
class HistoryFragment : DialogFragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            NexoViewModelFactory((requireActivity().application as NexoPayApp).container)
        )[DashboardViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        Dialog(requireContext(), R.style.Theme_NexoPay)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentHistoryBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = requireNotNull(_binding)
        val adapter = TransactionAdapter(expandedDetails = true)
        binding.recyclerTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTransactions.adapter = adapter
        binding.buttonBack.setOnClickListener { dismiss() }
        binding.buttonRefresh.setOnClickListener { viewModel.refresh() }

        val horizontalPadding = (20 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(bars.left + horizontalPadding, bars.top, bars.right + horizontalPadding, bars.bottom)
            insets
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.transactions)
                    binding.textCount.text = getString(R.string.history_count, state.transactions.size)
                    binding.textEmpty.isVisible = state.transactions.isEmpty() && !state.isLoading
                    binding.progressHistory.isVisible = state.transactions.isEmpty() && state.isLoading
                    binding.buttonRefresh.isEnabled = !state.isLoading && !state.isOperating
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        view?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onDestroyView() {
        _binding?.recyclerTransactions?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "history"
    }
}
