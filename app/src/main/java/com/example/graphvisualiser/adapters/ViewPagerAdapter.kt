package com.example.graphvisualiser.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(private var fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private var fragments : ArrayList<Fragment> = ArrayList()

    fun addFragment(fragment: Fragment){
        fragments.add(fragment)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}