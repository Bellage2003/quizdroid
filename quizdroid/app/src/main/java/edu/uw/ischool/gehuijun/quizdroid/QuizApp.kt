package edu.uw.ischool.gehuijun.quizdroid

import android.app.Application
import android.util.Log
import android.content.Context
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
            downloadData(urlString)
        } finally {
            isRunning = false
        }
    }

    private fun downloadData(urlString: String?): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            //connection.requestMethod = "GET"
            //connection.connect()

            val inputStream = connection.inputStream
            val reader = InputStreamReader(inputStream)
            reader.use {
                it.readText()
            }
        } catch (e: IOException) {
            throw IOException("Error downloading data", e)
        }
    }
}

class JsonTopicRepository(private val context: Context) : TopicRepository {

    internal fun parseJson(jsonData: String): List<Topic> {
        // Log the received JSON data
        Log.d("JsonTopicRepository", "Received JSON: $jsonData")

        // Parse the JSON data using Gson
        return try {
            Gson().fromJson(jsonData, object : TypeToken<List<Topic>>() {}.type)
        } catch (e: Exception) {
            // Log any parsing errors
            Log.e("JsonTopicRepository", "Error parsing JSON", e)
            emptyList()
        }
    }

    override suspend fun getTopics(): List<Topic> {
        val dataUrl = "http://tednewardsandbox.site44.com/questions.json"

        if (DownloadTask.isRunning) {
            return emptyList()
        }

        return try {
            withContext(Dispatchers.IO) {
                val jsonData = DownloadTask(context).execute(dataUrl)
                parseJson(jsonData)
            }
        } catch (e: Exception) {
            Log.e("JsonTopicRepository", "Error fetching topics", e)
            emptyList()
        }
    }
}

class QuizApp : Application() {
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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val topics = topicRepository.getTopics()
                Log.d("MainActivity", "Fetched topics: $topics")
                // Do something with the topics
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching topics", e)
            }
        }
    }
}
