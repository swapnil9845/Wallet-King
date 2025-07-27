package com.example.walletking.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.walletking.ui.dashboard.DashboardFragment
import com.example.walletking.ui.transactions.TransactionsFragment

class FinancePagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> DashboardFragment()
            1 -> TransactionsFragment()
            // 2 -> CategoriesFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}