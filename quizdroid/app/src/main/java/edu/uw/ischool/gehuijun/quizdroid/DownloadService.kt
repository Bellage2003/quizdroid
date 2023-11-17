package edu.uw.ischool.gehuijun.quizdroid

import android.app.AlertDialog
import android.app.IntentService
import android.content.Intent
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess


class DownloadService : IntentService("DownloadService") {

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val dataUrl = intent.getStringExtra(EXTRA_URL)
            CoroutineScope(Dispatchers.IO).launch {
                downloadAndSaveData(dataUrl)
            }
        }
    }

    private suspend fun downloadAndSaveData(urlString: String?) {
        try {
            if (NetworkUtils.isNetworkAvailable(this)) {
                val jsonData = DownloadTask(this).execute(urlString)
                if (jsonData.isNotEmpty()) {
                    JsonTopicRepository(this).saveJsonToFile(jsonData)
                    Log.d("DownloadService", "Data downloaded and saved successfully")
                    showToast("Data updated successfully")
                } else {
                    Log.e("DownloadService", "Failed to download data")
                    //showToast("Failed to download data. Retry or try again later.")
                    promptRetryOrQuit(urlString)
                }
            } else {
                Log.e("DownloadService", "No network available")
            }
        } catch (e: Exception) {
            Log.e("DownloadService", "Error during data download", e)
            promptRetryOrQuit(urlString)
        }
    }

    private suspend fun promptRetryOrQuit(urlString: String?) {
        // Show an AlertDialog on the UI thread
        withContext(Dispatchers.Main) {
            val builder = AlertDialog.Builder(this@DownloadService)
            builder.setTitle("Error Downloading Data")
            builder.setMessage("Failed to download data. Do you want to retry or quit the application?")
            builder.setPositiveButton("Retry") { _, _ ->
                // User clicked Retry, attempt the download again
                CoroutineScope(Dispatchers.IO).launch {
                    downloadAndSaveData(urlString)
                }
            }
            builder.setNegativeButton("Quit") { _, _ ->
                // User clicked Quit, close the application
                exitProcess(0)
            }
            builder.show()
        }
    }

    private suspend fun showToast(message: String) {
        // Show a Toast on the UI thread
        withContext(Dispatchers.Main) {
            Toast.makeText(this@DownloadService, message, Toast.LENGTH_SHORT).show()
        }
    }
}
