package com.example.graphvisualiser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.graphvisualiser.adapters.ViewPagerAdapter
import com.example.graphvisualiser.fragments.onboarding.OnboardingFragment
import com.example.graphvisualiser.fragments.onboarding.LastFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager2 : ViewPager2 = findViewById(R.id.onboardingViewPager)
        val viewPagerAdapter : ViewPagerAdapter = ViewPagerAdapter(this)


        viewPagerAdapter.addFragment(OnboardingFragment(0))
        viewPagerAdapter.addFragment(OnboardingFragment(1))
        viewPagerAdapter.addFragment(OnboardingFragment(2))
        viewPagerAdapter.addFragment(LastFragment())

        viewPager2.adapter = viewPagerAdapter

        val tabLayout : TabLayout = findViewById(R.id.onboardingTabLayout)
        TabLayoutMediator(tabLayout, viewPager2, true
        ) { tab, position -> viewPager2.currentItem = tab.position }.attach()


    }
}