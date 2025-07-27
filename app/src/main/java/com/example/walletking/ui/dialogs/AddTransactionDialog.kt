package com.example.walletking.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.walletking.R
import com.example.walletking.data.model.Category
import com.example.walletking.data.model.Transaction
import com.example.walletking.data.model.TransactionType
import com.example.walletking.databinding.FragmentAddTransactionDialogBinding
import com.example.walletking.ui.viewModel.FinanceViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch


class AddTransactionDialog:DialogFragment(){
    private var _binding:FragmentAddTransactionDialogBinding?=null
    private val binding  get()=_binding!!


    private val viewModel: FinanceViewModel by viewModels({ requireActivity() })
    private var currentType = TransactionType.EXPENSE
    private var categories = listOf<Category>()
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Convert 350dp to pixels
            val width = (350 * resources.displayMetrics.density).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setWindowAnimations(R.style.DialogAnimation)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use view binding to access layout elements
        _binding = FragmentAddTransactionDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel


        setupInitialState()
        setupTypeSelection()
        setupCategorySpinner()
        setupButtons()
        observeCategories()
    }
    private fun setupInitialState() {
        // Log initial state
        Log.d("AddTransaction", "Initial type: $currentType")

        // Set initial tab based on currentType
        val initialTab = when (currentType) {
            TransactionType.INCOME -> 0
            TransactionType.EXPENSE -> 1
        }
        binding.typeTabLayout.getTabAt(initialTab)?.select()
    }

    private fun setupTypeSelection() {
        binding.typeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newType = when(tab?.position) {
                    0 -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }

                // Log tab selection
                Log.d("AddTransaction", "Tab selected: ${tab?.position}, New type: $newType")

                if (newType != currentType) {
                    currentType = newType
                    observeCategories()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    private fun observeCategories() {
        // Observe categories based on selected type
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCategoriesByType(currentType).collect { newCategories ->
                categories = newCategories
                updateCategorySpinner()
            }
        }
    }  private fun updateCategorySpinner() {
        // Update category dropdown with new categories
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu,
            categories.map { it.name }
        )
        binding.spinnerCategory.setAdapter(adapter)
    }
    private fun setupCategorySpinner() {
        // Initialize category dropdown with empty adapter
        val adapter = ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_menu,
            mutableListOf()
        )
        binding.spinnerCategory.setAdapter(adapter)
    }
    private fun setupButtons() {
        // Handle save button click
        binding.btnSave.setOnClickListener {
            // Validate and save the transaction
            if (validateInput()) {
                saveTransaction()
                dismiss()
            }
        }

        // Handle cancel button click
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
    private fun validateInput(): Boolean {
        var isValid = true

        // Validate amount
        val amount = binding.etAmount.text.toString()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null) {
            binding.etAmount.error = getString(R.string.error_invalid_amount)
            isValid = false
        }

        // Validate description
        if (binding.etDescription.text.toString().isEmpty()) {
            binding.etDescription.error = getString(R.string.error_empty_description)
            isValid = false
        }

        // Validate category selection
        if (binding.spinnerCategory.text.toString().isEmpty()) {
            binding.spinnerCategory.error = getString(R.string.error_select_category)
            isValid = false
        }

        return isValid
    }
    private fun saveTransaction() {
        val amount = binding.etAmount.text.toString().toDouble()
        val description = binding.etDescription.text.toString()
        val category = binding.spinnerCategory.text.toString()

        // Log transaction creation
        Log.d("AddTransaction", "Saving transaction with type: $currentType")

        val transaction = Transaction(
            amount = amount,
            description = description,
            category = category,
            type = currentType,
            date = System.currentTimeMillis()
        )

        viewModel.addTransaction(transaction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}