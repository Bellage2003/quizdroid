package edu.uw.ischool.gehuijun.quizdroid

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import android.Manifest


class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var topicNameTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var shortDescriptionTextView: TextView
    private lateinit var totalQuestionsTextView: TextView
    private lateinit var beginButton: Button
    private lateinit var questionText: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var answerText: TextView
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button
    private lateinit var backButton: Button
    private lateinit var iconImageView: ImageView
    private lateinit var userAnswers: MutableList<String>
    private lateinit var preferencesButton: Button

    private var currentQuestionIndex = 0
    private var currentQuestions: List<Quiz> = emptyList()
    private var mScore = 0
    private var incorrectCount = 0
    private val url = "http://tednewardsandbox.site44.com/questions.json"

    companion object {
        private const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.topicListView)
        topicNameTextView = findViewById(R.id.topicNameTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        shortDescriptionTextView = findViewById(R.id.shortDescriptionTextView)
        totalQuestionsTextView = findViewById(R.id.totalQuestionsTextView)
        beginButton = findViewById(R.id.beginButton)
        questionText = findViewById(R.id.questionText)
        radioGroup = findViewById(R.id.radioGroup)
        submitButton = findViewById(R.id.submitButton)
        answerText = findViewById(R.id.answerText)
        nextButton = findViewById(R.id.nextButton)
        finishButton = findViewById(R.id.finishButton)
        backButton = findViewById(R.id.backButton)
        iconImageView = findViewById(R.id.iconImageView)
        userAnswers = mutableListOf()
        preferencesButton = findViewById(R.id.preferencesButton)

        val file = File("/sdcard/questions.json")

        // Check for READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_PERMISSION_CODE
            )
        } else {
            // Permission is granted, proceed with reading the file
            loadTopicsFromFile(file)
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val jsonData = readFileContent(file)
                val topics = JsonTopicRepository(this@MainActivity).parseJson(jsonData)
                val topicsArray = topics.map { it.title }.toTypedArray()
                val adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        topicsArray
                    )
                listView.adapter = adapter
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading topics", e)
                Toast.makeText(this@MainActivity, "Error loading topics", Toast.LENGTH_SHORT).show()
            }
        }


        preferencesButton.setOnClickListener {
            showPreferences()
        }
        loadDataFromPreferences()

        listView.setOnItemClickListener { _, _, position, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                val selectedTitle = listView.getItemAtPosition(position) as String
                val topic = withContext(Dispatchers.IO) {
                    QuizApp.getTopicRepository().getTopics().find { it.title == selectedTitle }
                }
                listView.visibility = ListView.GONE

                topicNameTextView.text = topic?.title ?: ""
                topicNameTextView.visibility = TextView.VISIBLE

                descriptionTextView.text = topic?.desc ?: ""
                descriptionTextView.visibility = TextView.VISIBLE

                totalQuestionsTextView.text = "Total Questions: ${topic?.questions?.size ?: 0}"
                totalQuestionsTextView.visibility = TextView.VISIBLE

                beginButton.visibility = Button.VISIBLE
                beginButton.setOnClickListener {
                    topic?.questions?.let { loadQuestion(it) }
                    topicNameTextView.visibility = TextView.GONE
                    beginButton.visibility = Button.GONE
                    descriptionTextView.visibility = TextView.GONE
                    totalQuestionsTextView.visibility = TextView.GONE

                    questionText.visibility = TextView.VISIBLE
                    radioGroup.visibility = RadioGroup.VISIBLE
                    submitButton.visibility = Button.GONE
                    answerText.visibility = TextView.GONE
                    iconImageView.visibility = ImageView.GONE
                }

                radioGroup.setOnCheckedChangeListener { _, checkedId ->
                    if (checkedId != -1) {
                        submitButton.visibility = Button.VISIBLE
                        backButton.visibility = Button.VISIBLE
                    } else {
                        submitButton.visibility = Button.GONE
                        backButton.visibility = Button.GONE
                    }
                }

                submitButton.setOnClickListener {
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
                    val selectedAnswer = selectedRadioButton.text.toString()

                    userAnswers.add(selectedAnswer)

                    topic?.questions?.let { loadQuestion(it) }

                    showAnswerPage()
                }
            }
        }

        nextButton.setOnClickListener {
            currentQuestionIndex++
            loadQuestion(currentQuestions)

            radioGroup.clearCheck()

            answerText.visibility = TextView.GONE
            nextButton.visibility = Button.GONE

            radioGroup.visibility = RadioGroup.VISIBLE
            answerText.visibility = TextView.GONE
        }

        finishButton.setOnClickListener {
            userAnswers = mutableListOf()
            mScore = 0
            incorrectCount = 0
            currentQuestionIndex = 0

            listView.visibility = ListView.VISIBLE
            topicNameTextView.visibility = TextView.GONE
            beginButton.visibility = Button.GONE
            questionText.visibility = TextView.GONE
            radioGroup.visibility = RadioGroup.GONE
            submitButton.visibility = Button.GONE
            answerText.visibility = TextView.GONE
            nextButton.visibility = Button.GONE
            finishButton.visibility = Button.GONE
            backButton.visibility = Button.GONE
            answerText.visibility = TextView.GONE
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

    }

    private fun showPreferences() {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }

    private fun loadQuestion(questions: List<Quiz>) {
        val currentQuestion = questions[currentQuestionIndex]

        topicNameTextView.visibility = TextView.GONE
        descriptionTextView.visibility = TextView.GONE
        totalQuestionsTextView.visibility = TextView.GONE
        beginButton.visibility = Button.GONE
        radioGroup.visibility = RadioGroup.VISIBLE

        questionText.text = currentQuestion.text

        radioGroup.removeAllViews()

        currentQuestion.answers.forEachIndexed { index, choice ->
            val radioButton = RadioButton(this@MainActivity)
            radioButton.text = choice
            radioButton.id = index // Set the index as the id
            radioGroup.addView(radioButton)
        }

        currentQuestions = questions
    }

    private fun showAnswerPage() {
        radioGroup.visibility = RadioGroup.GONE
        submitButton.visibility = Button.GONE
        answerText.visibility = TextView.VISIBLE

        val currentQuestion = currentQuestions[currentQuestionIndex]
        val correctAnswerIndex = currentQuestion.answer
        val correctAnswer = currentQuestion.answers[correctAnswerIndex]

        val userAnswer = userAnswers[currentQuestionIndex]
        val isCorrect = correctAnswer == userAnswer

        answerText.text = "Your answer: $userAnswer\nCorrect answer: $correctAnswer"

        if (isCorrect) {
            mScore++
            answerText.append("\nYou have $mScore out of ${currentQuestions.size} correct.")
        } else {
            incorrectCount++
            answerText.append("\nYou have $mScore out of ${currentQuestions.size} correct.")
        }

        if (currentQuestionIndex < currentQuestions.size - 1) {
            nextButton.visibility = Button.VISIBLE
        } else {
            finishButton.visibility = Button.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (currentQuestionIndex == 0) {
            listView.visibility = ListView.VISIBLE
            questionText.visibility = TextView.GONE
            radioGroup.visibility = RadioGroup.GONE
            submitButton.visibility = Button.GONE
            backButton.visibility = Button.GONE
            answerText.visibility = TextView.GONE
            nextButton.visibility = Button.GONE
            finishButton.visibility = Button.GONE
        } else {
            currentQuestionIndex--
            answerText.visibility = TextView.GONE
            finishButton.visibility = Button.GONE
            loadQuestion(currentQuestions)
        }
    }

    private fun loadDataFromPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val dataUrl = sharedPreferences.getString("pref_data_url", url)
        val refreshInterval = sharedPreferences.getString("pref_refresh_interval", "60")?.toLongOrNull()
    }

    private fun readFileContent(file: File): String {
        val reader = BufferedReader(FileReader(file))
        val content = StringBuilder()
        var line: String?

        try {
            while (reader.readLine().also { line = it } != null) {
                content.append(line)
            }
        } finally {
            reader.close()
        }

        return content.toString()
    }

    private fun loadTopicsFromFile(file: File) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val jsonData = readFileContent(file)
                val topics = JsonTopicRepository(this@MainActivity).parseJson(jsonData)
                val topicsArray = topics.map { it.title }.toTypedArray()
                val adapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, topicsArray)
                listView.adapter = adapter
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading topics", e)
                // Handle error loading topics (e.g., show a toast or log a message)
                Toast.makeText(this@MainActivity, "Error loading topics", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with reading the file
                    val file = File("/sdcard/questions.json")
                    loadTopicsFromFile(file)
                } else {
                    // Permission denied, handle accordingly (e.g., show a toast or log a message)
                    Toast.makeText(
                        this,
                        "Permission denied. Cannot load topics.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}




