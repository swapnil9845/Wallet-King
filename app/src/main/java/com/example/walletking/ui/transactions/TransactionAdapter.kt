package com.example.walletking.ui.transactions

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.walletking.R
import com.example.walletking.data.model.Transaction
import com.example.walletking.data.model.TransactionType
import com.example.walletking.databinding.ItemTransactionBinding

import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val onUndoDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {
    private var recyclerView: RecyclerView? = null
//    private val itemsPendingRemoval = mutableSetOf<Transaction>()

    //ListAdapter automates diffing between old and new lists using DiffUtil and helps refresh only the necessary items.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        setupSwipeToDelete(recyclerView)
    }
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }


        }

        fun bind(transaction: Transaction) {
            binding.apply {
                tvDescription.text = transaction.description
                tvCategory.text = transaction.category
                tvDate.text = dateFormatter.format(Date(transaction.date))

                ivCategoryIcon.setImageResource(getCategoryIcon(transaction.category))


                // Format amount with currency symbol and color based on transaction type
                val amount = currencyFormatter.format(transaction.amount)
                tvAmount.text = when (transaction.type) {
                    TransactionType.INCOME -> "+$amount"
                    TransactionType.EXPENSE -> "-$amount"
                }

                // Set text color based on transaction type
                tvAmount.setTextColor(
                    tvAmount.context.getColor(
                        when (transaction.type) {
                            TransactionType.INCOME -> R.color.income_green
                            TransactionType.EXPENSE ->R.color.expense_red
                        }
                    )
                )
            }
        }
    }
    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, // Drag directions - none in this case
            ItemTouchHelper.RIGHT // Swipe directions - right only
        ) {
            private val background = ColorDrawable(Color.RED)
            private val paint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                textAlign = Paint.Align.RIGHT
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = getItem(position)
                animateItemRemoval(viewHolder.itemView) {
                    // Remove the item from the adapter
                    val currentList = currentList.toMutableList()
                    currentList.removeAt(position)
                    submitList(currentList)

                    // Call the delete callback
                    onDeleteClick(transaction)

                    // Show undo Snackbar
                    recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.let { view ->
                        Snackbar.make(
                            view,
                            "Transaction deleted",
                            Snackbar.LENGTH_LONG
                        ).setAction("UNDO") {
                            // Restore the item
                            val restoredList = currentList.toMutableList()
                            restoredList.add(position, transaction)
                            submitList(restoredList)
                            onUndoDelete(transaction)
                        }.setActionTextColor(Color.WHITE)
                            .show()
                    }
                }
            }
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                // Apply fade effect during swipe
                val alpha = 1.0f - Math.abs(dX) / itemView.width
                itemView.alpha = alpha

                // Draw background with slide effect
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                background.draw(c)

                // Draw delete icon with fade
                val deleteIcon = ContextCompat.getDrawable(
                    recyclerView.context,
                    R.drawable.ic_delete
                )
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + deleteIcon.intrinsicWidth
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                deleteIcon.alpha = (alpha * 255).toInt()
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun animateItemRemoval(view: android.view.View, onComplete: () -> Unit) {
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.alpha = value
                view.translationX = (1f - value) * view.width
                if (value == 0f) {
                    onComplete()
                }
            }
        }
        animator.start()
    }
    private fun animateItemRestoration(view: android.view.View, onComplete: () -> Unit) {
        view.alpha = 0f
        view.translationX = view.width.toFloat()

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.alpha = value
                view.translationX = (1f - value) * view.width
                if (value == 1f) {
                    onComplete()
                }
            }
        }
        animator.start()
    }
    private fun getCategoryIcon(category: String): Int {
        return when (category.trim().lowercase()) {
            "food & dining", "food", "dining", "groceries" -> R.drawable.ic_food
            "shopping", "clothes" -> R.drawable.ic_shopping
            "transportation", "travel", "gas" -> R.drawable.ic_transport
            "bills & utilities", "utilities", "bills", "rent" -> R.drawable.ic_utilities
            "healthcare", "medical" -> R.drawable.ic_healthcare
            "salary", "income", "wages" -> R.drawable.ic_salary
            "investments", "stocks", "dividends" -> R.drawable.ic_investment
            "freelance", "freelancing", "contract work" -> R.drawable.ic_freelance
            else -> R.drawable.ic_misc
        }
    }


    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}