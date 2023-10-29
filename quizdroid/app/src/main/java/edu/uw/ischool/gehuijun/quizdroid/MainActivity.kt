package edu.uw.ischool.gehuijun.quizdroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

data class Question(val questionText: String, val choices: List<String>, val correctAnswer: String)

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var topicNameTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var totalQuestionsTextView: TextView
    private lateinit var beginButton: Button
    private lateinit var questionText: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var answerText: TextView
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button
    private lateinit var backButton: Button
    private lateinit var userAnswers: MutableList<String>

    private var currentQuestionIndex = 0
    private var currentQuestions: List<Question> = emptyList()
    private var mScore = 0
    private var incorrectCount = 0

    private val mathQuestions = listOf(
        Question("What is 2 + 2?", listOf("1", "2", "3", "4"), "4"),
        Question("What is 3 x 3?", listOf("6", "9", "12", "15"), "9"),
        Question("What is the square root of 144?", listOf("14","16","18", "12"), "12"),
        Question("What is the value of π (pi) to two decimal places?",listOf("3.16", "3.14", "3.20", "3.18"), "3.14")
    )

    private val physicsQuestions = listOf(
        Question("What is the speed of light?", listOf("299,792,458 m/s", "100 m/s", "1,000,000 m/s", "10 m/s"), "299,792,458 m/s"),
        Question("What is Newton's first law of motion?", listOf("An object at rest stays at rest", "F=ma", "For every action, there is an equal and opposite reaction", "None of the above"), "An object at rest stays at rest"),
        Question("What is the unit of measurement for electric current?",listOf("Volt (V)", "Ohm (Ω)", "Ampere (A)", "Watt (W)"), "Ampere (A)"),
        Question("What is the SI unit of force?",listOf("Newton (N)", "Joule (J)", "Watt (W)", "Volt (V)"), "Newton (N)")
    )

    private val marvelQuestions = listOf(
        Question("Who is Thor's brother?", listOf("Loki", "Iron Man", "Captain America", "Hulk"), "Loki"),
        Question("What is Iron Man's real name?", listOf("Tony Stark", "Steve Rogers", "Bruce Banner", "Peter Parker"), "Tony Stark"),
        Question("What is the real name of Captain America?",listOf("Steve Rogers", "Tony Stark", "Bruce Banner", "Peter Parker"), "Steve Rogers"),
        Question("Who is known as the 'Merc with a Mouth' in the Marvel Universe?",listOf("Iron Man", "Deadpool", "Wolverine", "Thor"), "Deadpool")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.topicListView)
        topicNameTextView = findViewById(R.id.topicNameTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        totalQuestionsTextView = findViewById(R.id.totalQuestionsTextView)
        beginButton = findViewById(R.id.beginButton)
        questionText = findViewById(R.id.questionText)
        radioGroup = findViewById(R.id.radioGroup)
        submitButton = findViewById(R.id.submitButton)
        answerText = findViewById(R.id.answerText)
        nextButton = findViewById(R.id.nextButton)
        finishButton = findViewById(R.id.finishButton)
        backButton = findViewById(R.id.backButton)
        userAnswers = mutableListOf()

        // Set up topics list
        val topics = arrayOf("Math", "Physics", "Marvel Super Heroes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, topics)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedTopic = topics[position]

            topicNameTextView.text = selectedTopic
            topicNameTextView.visibility = TextView.VISIBLE

            val topicDescription = when(selectedTopic) {
                "Physics" -> "Science that deals with the structure of matter and the interactions between the fundamental constituents of the observable universe. Physics, the most fundamental of the natural sciences, focuses on the structure of matter and the interactions between the fundamental constituents of the observable universe."
                "Math" -> "The science and study of quality, structure, space, and change. Mathematicians seek out patterns, formulate new conjectures, and establish truth by rigorous deduction from appropriately chosen axioms and definitions."
                "Marvel Super Heroes" -> "Iconic fictional characters created by Marvel Comics. These superheroes possess extraordinary powers or abilities and often use them to protect the world from powerful threats and villains."
                else -> ""
            }
            descriptionTextView.text = topicDescription
            descriptionTextView.visibility = TextView.VISIBLE

            val totalQuestions = when(selectedTopic) {
                "Math" -> mathQuestions.size
                "Physics" -> physicsQuestions.size
                "Marvel Super Heroes" -> marvelQuestions.size
                else -> 0
            }
            totalQuestionsTextView.text = "Total Questions: $totalQuestions"
            totalQuestionsTextView.visibility = TextView.VISIBLE

            beginButton.visibility = Button.VISIBLE
            beginButton.setOnClickListener {
                // Load questions based on selected topic
                when (selectedTopic) {
                    "Math" -> loadQuestion(mathQuestions)
                    "Physics" -> loadQuestion(physicsQuestions)
                    "Marvel Super Heroes" -> loadQuestion(marvelQuestions)
                }
                topicNameTextView.visibility = TextView.GONE
                beginButton.visibility = Button.GONE
                descriptionTextView.visibility = TextView.GONE
                totalQuestionsTextView.visibility = TextView.GONE

                questionText.visibility = TextView.VISIBLE
                radioGroup.visibility = RadioGroup.VISIBLE
                submitButton.visibility = Button.GONE
            }

            radioGroup.setOnCheckedChangeListener { group, checkedId ->
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

                currentQuestions = when (selectedTopic) {
                    "Math" -> mathQuestions
                    "Physics" -> physicsQuestions
                    "Marvel Super Heroes" -> marvelQuestions
                    else -> emptyList()
                }

                showAnswerPage()
            }
        }

        nextButton.setOnClickListener {
            // Load the next question and choices here
            currentQuestionIndex++
            loadQuestion(currentQuestions)

            // Clear radio button selection
            radioGroup.clearCheck()

            answerText.visibility = TextView.GONE
            nextButton.visibility = Button.GONE

            radioGroup.visibility = RadioGroup.VISIBLE
        }

        finishButton.setOnClickListener {
            // Reset
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
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

    }

    private fun loadQuestion(questions: List<Question>) {
        val currentQuestion = questions[currentQuestionIndex]
        topicNameTextView.visibility = TextView.GONE
        descriptionTextView.visibility = TextView.GONE
        totalQuestionsTextView.visibility = TextView.GONE
        beginButton.visibility = Button.GONE
        radioGroup.visibility = RadioGroup.VISIBLE

        // Set question text
        val questionText = findViewById<TextView>(R.id.questionText)
        questionText.text = currentQuestion.questionText

        // Clear existing radio buttons
        radioGroup.removeAllViews()

        // Add new radio buttons for choices
        for (choice in currentQuestion.choices) {
            val radioButton = RadioButton(this)
            radioButton.text = choice
            radioGroup.addView(radioButton)
        }
    }

    private fun showAnswerPage() {
        radioGroup.visibility = RadioGroup.GONE
        submitButton.visibility = Button.GONE
        answerText.visibility = TextView.VISIBLE

        val currentQuestion = currentQuestions[currentQuestionIndex]
        val correctAnswer = currentQuestion.correctAnswer

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
            // If on the first question, go back to topic list
            listView.visibility = ListView.VISIBLE
            questionText.visibility = TextView.GONE
            radioGroup.visibility = RadioGroup.GONE
            submitButton.visibility = Button.GONE
            backButton.visibility = Button.GONE
            answerText.visibility = TextView.GONE
            nextButton.visibility = Button.GONE
            finishButton.visibility = Button.GONE
        } else {
            // If not on the first question, go back to the previous question
            currentQuestionIndex--
            loadQuestion(currentQuestions)
        }
    }
}