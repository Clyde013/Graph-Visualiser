package com.example.graphvisualiser.fragments.onboarding

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.graphvisualiser.R

class OnboardingFragment(private val frag: Int): Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgRes: Int = when (frag){
            0 -> R.drawable.onboarding0
            1 -> R.drawable.onboarding1
            2 -> R.drawable.onboarding2
            else -> R.drawable.onboarding0
        }

        val textRes: Int = when (frag){
            0 -> R.string.onboarding_fragment_0
            1 -> R.string.onboarding_fragment_1
            2 -> R.string.onboarding_fragment_2
            else -> R.string.onboarding_fragment_0
        }

        view.findViewById<ImageView>(R.id.onBoardingImageView).setImageResource(imgRes)
        view.findViewById<TextView>(R.id.onboardingTextView).text = resources.getText(textRes)
        Log.i("onboarding set text", "${view.findViewById<TextView>(R.id.onboardingTextView)}, ${resources.getText(textRes)}")
    }
}