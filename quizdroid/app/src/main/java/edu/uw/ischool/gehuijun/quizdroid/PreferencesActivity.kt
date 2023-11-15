package edu.uw.ischool.gehuijun.quizdroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Add listeners or additional setup for preferences if needed
            findPreference<Preference>("data_url")?.setOnPreferenceChangeListener { _, newValue ->
                true
            }

            findPreference<Preference>("download_interval")?.setOnPreferenceChangeListener { _, newValue ->
                true
            }
        }
    }
}