package com.example.graphvisualiser

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PreferencesFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_preferences, container, false)
        val preferenceFragmentContainerView = root.findViewById<FragmentContainerView>(R.id.preferenceFragmentContainerView)

        childFragmentManager.beginTransaction()
            .replace(preferenceFragmentContainerView.id, MyPreferenceFragment())
            .commit()

        return root
    }


    class MyPreferenceFragment: PreferenceFragmentCompat(){
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // all values in the preferences fragment are saved to SharedPreferences
            // and will be retained even if app is closed
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
            findPreference<Preference>("name_pref")?.summary = "name"   // change displayed values like this (will be updated in SharedPrefs)
            print(sharedPref?.getBoolean("switch_pref", false)) // access stored values like this
        }
    }
}