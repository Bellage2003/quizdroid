package edu.uw.ischool.gehuijun.quizdroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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

    private var currentQuestionIndex = 0
    private var currentQuestions: List<Quiz> = emptyList()
    private var mScore = 0
    private var incorrectCount = 0

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

        val topics = QuizApp.getTopicRepository().getTopics().map {"${it.title}\n(${it.shortDescription})" }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, topics)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedTopic = topics[position]
            val selectedTitle = selectedTopic.split("\n(")[0]

            val topic = QuizApp.getTopicRepository().getTopics().find { it.title == selectedTitle }
            listView.visibility = ListView.GONE

            val iconResId = topic?.icon ?: R.drawable.baseline_quiz_24
            iconImageView.setImageResource(iconResId)
            //iconImageView.visibility = ImageView.VISIBLE

            topicNameTextView.text = topic?.title ?: ""
            topicNameTextView.visibility = TextView.VISIBLE

            descriptionTextView.text = topic?.longDescription ?: ""
            descriptionTextView.visibility = TextView.VISIBLE

            totalQuestionsTextView.text = "Total Questions: ${topic?.quizzes?.size ?: 0}"
            totalQuestionsTextView.visibility = TextView.VISIBLE

            beginButton.visibility = Button.VISIBLE
            beginButton.setOnClickListener {
                topic?.quizzes?.let { loadQuestion(it) }
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

                topic?.quizzes?.let { loadQuestion(it) }

                showAnswerPage()
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

    private fun loadQuestion(questions: List<Quiz>) {
        val currentQuestion = questions[currentQuestionIndex]

        topicNameTextView.visibility = TextView.GONE
        descriptionTextView.visibility = TextView.GONE
        totalQuestionsTextView.visibility = TextView.GONE
        beginButton.visibility = Button.GONE
        radioGroup.visibility = RadioGroup.VISIBLE

        questionText.text = currentQuestion.questionText

        radioGroup.removeAllViews()

        currentQuestion.choices.forEachIndexed { index, choice ->
            val radioButton = RadioButton(this)
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
        val correctAnswerIndex = currentQuestion.correctAnswer
        val correctAnswer = currentQuestion.choices[correctAnswerIndex]

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
}
