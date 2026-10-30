package com.uc1.nexopay.ui.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uc1.nexopay.R
import com.uc1.nexopay.data.local.TransactionEntity
import com.uc1.nexopay.data.local.TransactionType
import com.uc1.nexopay.databinding.ItemTransactionBinding
import com.uc1.nexopay.ui.common.UiUtils

class TransactionAdapter(private val expandedDetails: Boolean = false) :
    ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DIFF) {

    var onNewFirstItem: (() -> Unit)? = null

    override fun onCurrentListChanged(
        previousList: List<TransactionEntity>,
        currentList: List<TransactionEntity>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        val first = currentList.firstOrNull() ?: return
        // Las actualizaciones de saldo o de un comprobante existente no desplazan la lista.
        if (previousList.none { it.id == first.id }) onNewFirstItem?.invoke()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        if (expandedDetails) {
            binding.textTitle.maxLines = 2
            binding.textSubtitle.maxLines = 3
        }
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            val context = binding.root.context
            val income = TransactionType.isIncome(transaction.type)

            val accent = ContextCompat.getColor(
                context,
                if (income) R.color.nexo_success else R.color.nexo_danger
            )
            val container = ContextCompat.getColor(
                context,
                if (income) R.color.nexo_success_container else R.color.nexo_danger_container
            )

            binding.textTitle.text = UiUtils.transactionTitle(transaction)
            binding.textSubtitle.text = UiUtils.transactionSubtitle(transaction)
            binding.textAmount.text = UiUtils.signedAmount(transaction)
            binding.textAmount.setTextColor(accent)

            binding.cardIcon.setCardBackgroundColor(container)
            binding.imageIcon.setImageResource(iconFor(transaction.type))
            binding.imageIcon.imageTintList = ColorStateList.valueOf(accent)
        }

        private fun iconFor(type: String): Int = when (type) {
            TransactionType.DEPOSIT -> R.drawable.ic_deposit
            TransactionType.WITHDRAWAL -> R.drawable.ic_withdraw
            TransactionType.TRANSFER_SENT,
            TransactionType.TRANSFER_RECEIVED -> R.drawable.ic_transfer
            TransactionType.SERVICE_PAYMENT -> R.drawable.ic_service
            else -> R.drawable.ic_history
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionEntity>() {
            override fun areItemsTheSame(
                oldItem: TransactionEntity,
                newItem: TransactionEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: TransactionEntity,
                newItem: TransactionEntity
            ): Boolean = oldItem == newItem
        }
    }
}
