package edu.uw.ischool.gehuijun.quizdroid

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Application
import android.app.PendingIntent
import android.util.Log
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.system.exitProcess


data class Quiz(
    val text: String,
    val answer: Int,
    val answers: List<String>
)

data class Topic(
    val title: String,
    val desc: String,
    val questions: List<Quiz>
)

interface TopicRepository {
    suspend fun getTopics(): List<Topic>
}

class DownloadTask(private val context: Context) {

    companion object {
        var isRunning = false
    }

    fun execute(urlString: String?): String {
        isRunning = true
        return try {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val jsonData = downloadData(urlString)
                saveJsonToFile(jsonData)
                jsonData
            } else {
                Log.e("DownloadTask", "No network available")
                ""
            }
        } finally {
            isRunning = false
        }
    }

    private fun downloadData(urlString: String?): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            val inputStream = connection.inputStream
            val reader = InputStreamReader(inputStream)
            reader.use {
                it.readText()
            }
        } catch (e: IOException) {
            throw IOException("Error downloading data", e)
        }
    }

    private fun saveJsonToFile(jsonData: String) {
        try {
            val fileName = "questions.json"
            val file = File(context.filesDir, fileName)
            file.writeText(jsonData)
            Log.d("DownloadTask", "JSON data saved to file: $fileName")
        } catch (e: IOException) {
            Log.e("DownloadTask", "Error saving JSON data to file", e)
        }
    }
}

class JsonTopicRepository(private val context: Context) : TopicRepository {

    internal fun parseJson(jsonData: String): List<Topic> {
        if (NetworkUtils.isNetworkAvailable(context)) {
            Log.d("JsonTopicRepository", "Received JSON: $jsonData")

            return try {
                Gson().fromJson(jsonData, object : TypeToken<List<Topic>>() {}.type)
            } catch (e: Exception) {
                Log.e("JsonTopicRepository", "Error parsing JSON", e)
                emptyList()
            }
        } else {
            Log.e("JsonTopicRepository", "No network available")
            return emptyList()
        }
    }


    private fun readJsonFromFile(fileName: String): String {
        return try {
            // Specify the path to the file on the internal storage
            val filePath = File(context.filesDir, fileName)

            // Read the contents of the file
            if (filePath.exists()) {
                Log.d("JsonTopicRepository", "File exists at path: ${filePath.absolutePath}")
                filePath.readText()
            } else {
                Log.e("JsonTopicRepository", "File does not exist at path: ${filePath.absolutePath}")
                ""
            }
        } catch (e: Exception) {
            Log.e("JsonTopicRepository", "Error reading JSON from internal storage", e)
            ""
        }
    }

    internal fun saveJsonToFile(jsonData: String) {
        try {
            val fileName = "questions.json"
            // Specify the path to the file on the internal storage
            val filePath = File(context.filesDir, fileName)

            // Write the JSON data to the file
            filePath.writeText(jsonData)

            Log.d("JsonTopicRepository", "Saved JSON to file: ${filePath.absolutePath}")
        } catch (e: Exception) {
            Log.e("JsonTopicRepository", "Error saving JSON to file", e)
        }
    }

    override suspend fun getTopics(): List<Topic> {
        val fileName = "questions.json"

        return try {
            withContext(Dispatchers.IO) {
                val jsonData = readJsonFromFile(fileName)
                parseJson(jsonData)
            }
        } catch (e: Exception) {
            Log.e("JsonTopicRepository", "Error fetching topics", e)
            emptyList()
        }
    }
}

class QuizApp: Application() {
    private val topicRepository: TopicRepository = JsonTopicRepository(this)

    companion object {
        private lateinit var instance: QuizApp

        fun getInstance(): QuizApp {
            return instance
        }

        fun getTopicRepository(): TopicRepository {
            return getInstance().topicRepository
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("QuizApp", "QuizApp is running")

        if (NetworkUtils.isNetworkAvailable(applicationContext)) {
            // Display a Toast with the scheduled download URL
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val dataUrl = sharedPreferences.getString("data_url", "")
            Toast.makeText(this, "Scheduled download URL: $dataUrl", Toast.LENGTH_LONG).show()
        }
        // Schedule periodic download
        schedulePeriodicDownload()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load topics from the local file initially
                val topics = topicRepository.getTopics()
                Log.d("MainActivity", "Fetched topics from local file: $topics")

                // Attempt to download new data based on preferences
                downloadDataInBackground()

                // Do something with the topics
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching topics", e)
            }
        }
    }

    private fun schedulePeriodicDownload() {
        // Get the user-defined interval for periodic downloads from preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val downloadIntervalStr = sharedPreferences.getString("pref_refresh_interval", "60")
        val downloadInterval = downloadIntervalStr?.toLongOrNull() ?: 60

        // Schedule the download service using AlarmManager
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val pendingIntent = createDownloadServicePendingIntent()

            // Set the repeating alarm
            val intervalMillis = downloadInterval * 60 * 1000
            val initialDelayMillis = SystemClock.elapsedRealtime() + intervalMillis
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                initialDelayMillis,
                intervalMillis,
                pendingIntent
            )
        } else {
            Log.e("QuizApp", "AlarmManager is null. Unable to schedule periodic download.")
        }
    }


    private fun createDownloadServicePendingIntent(): PendingIntent {
        val intent = Intent(this, DownloadService::class.java)
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private suspend fun downloadDataInBackground() {
        try {
            // Notify user about the download attempt
            withContext(Dispatchers.Main) {
                Toast.makeText(this@QuizApp, "Attempting to download data...", Toast.LENGTH_SHORT).show()
            }

            withContext(Dispatchers.IO) {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@QuizApp)
                val dataUrl = sharedPreferences.getString("pref_data_url", "")
                val refreshInterval = sharedPreferences.getString("pref_refresh_interval", "")?.toLongOrNull()

                if (!dataUrl.isNullOrEmpty() && refreshInterval != null) {
                    // Calculate the time since the last update
                    val lastUpdateTime = sharedPreferences.getLong("last_update_time", 0)
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - lastUpdateTime

                    // Check if it's time to refresh based on the specified interval
                    if (elapsedTime >= refreshInterval * 60 * 1000) {
                        try {
                            // Attempt to download new data
                            val jsonData = DownloadTask(this@QuizApp).execute(dataUrl)
                            if (jsonData.isNotEmpty()) {
                                // Save the new data to the local file
                                JsonTopicRepository(this@QuizApp).saveJsonToFile(jsonData)

                                // Update the last update time
                                sharedPreferences.edit().putLong("last_update_time", currentTime).apply()

                                // Notify user about successful download
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@QuizApp, "Data updated successfully", Toast.LENGTH_SHORT).show()
                                }

                                // Notify the user or handle the updated data
                                Log.d("QuizApp", "Data updated successfully")
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@QuizApp, "Failed to download new data", Toast.LENGTH_SHORT).show()
                                }
                                Log.e("QuizApp", "Failed to download new data")
                            }
                        } catch (e: Exception) {
                            Log.e("QuizApp", "Error during data download", e)
                            throw e
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("QuizApp", "Error downloading data", e)

            // Display an AlertDialog for retry or quit
            withContext(Dispatchers.Main) {
                val builder = AlertDialog.Builder(this@QuizApp)
                builder.setTitle("Error Downloading Data")
                builder.setMessage("Failed to download data. Do you want to retry or quit the application?")
                builder.setPositiveButton("Retry") { _, _ ->
                    // User clicked Retry, attempt the download again
                    CoroutineScope(Dispatchers.IO).launch {
                        downloadDataInBackground()
                    }
                }
                builder.setNegativeButton("Quit") { _, _ ->
                    // User clicked Quit, close the application
                    exitProcess(0)
                }
                builder.show()
            }
        }
    }
}

class NetworkUtils {
    companion object {
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
    }
}
