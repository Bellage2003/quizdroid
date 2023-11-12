package edu.uw.ischool.gehuijun.quizdroid

import android.os.Bundle
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

        loadDataFromPreferences()

        CoroutineScope(Dispatchers.Main).launch {
            val jsonData = withContext(Dispatchers.IO) {
                DownloadTask(this@MainActivity).execute(url)
            }

            val topics = JsonTopicRepository(this@MainActivity).parseJson(jsonData)
            val topicsArray = topics.map { it.title }.toTypedArray()
            val adapter =
                ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, topicsArray)
            listView.adapter = adapter
        }

        preferencesButton.setOnClickListener {
            showPreferences()
        }

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
}


