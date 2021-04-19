package com.example.graphvisualiser.fragments.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.graphvisualiser.MainActivity
import com.example.graphvisualiser.R

class LastFragment: Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_onboarding_last, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.button_leave_onboarding).setOnClickListener {
            val leaveIntent : Intent = Intent(context, MainActivity::class.java)
            leaveIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(leaveIntent)
            activity?.finish()
            activity?.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}