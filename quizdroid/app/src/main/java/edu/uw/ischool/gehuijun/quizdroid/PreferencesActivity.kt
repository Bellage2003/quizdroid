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

            findPreference<Preference>("data_url")?.setOnPreferenceChangeListener { _, newValue ->
                // Use the utility class to schedule periodic download
                PeriodicDownloadScheduler(requireContext()).schedulePeriodicDownload(newValue.toString())
                true
            }

            findPreference<Preference>("download_interval")?.setOnPreferenceChangeListener { _, _ ->
                // Handle download interval change if needed
                true
            }
        }
    }
}
