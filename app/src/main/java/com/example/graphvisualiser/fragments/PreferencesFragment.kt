package com.example.graphvisualiser.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.example.graphvisualiser.R

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
            addPreferencesFromResource(R.xml.preferences)

            // all values in the preferences fragment are saved to SharedPreferences
            // and will be retained even if app is closed

            val switchPreference = findPreference<SwitchPreference>("mode_pref")
            switchPreference?.switchTextOn = "Dark Mode"
            switchPreference?.switchTextOff = "Light Mode"

            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
            switchPreference?.isChecked = sharedPref?.getBoolean("mode_pref", true)!!

        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            when (preference?.key){
                "mode_pref" -> {    // toggling light and dark mode
                    val switchPreference = preference as SwitchPreference
                    if (switchPreference.isChecked) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        Log.i("mode", "pref click dark mode")
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        Log.i("mode","pref click light mode")
                    }

                    val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
                    if (sharedPref != null) {
                        with(sharedPref.edit()) {
                            putBoolean("mode_pref", switchPreference.isChecked)
                            apply()
                        }
                    }
                }
            }

            return true
        }
    }
}