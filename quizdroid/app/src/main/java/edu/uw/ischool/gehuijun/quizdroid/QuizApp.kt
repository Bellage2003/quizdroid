package edu.uw.ischool.gehuijun.quizdroid

import android.app.Application
import android.util.Log

data class Quiz(val questionText: String, val choices: List<String>, val correctAnswer: Int)

data class Topic(
    val title: String,
    val icon: Int,
    val shortDescription: String,
    val longDescription: String,
    val quizzes: List<Quiz>
)

interface TopicRepository {
    fun getTopics(): List<Topic>
}

class InMemoryTopicRepository : TopicRepository {
    private val topics: List<Topic> = listOf(
        Topic(
            "Math",
            R.drawable.baseline_quiz_24,
            "Mathematical operations",
            "Mathematics is the science of numbers and their operations, interrelations, combinations, generalizations, and abstractions and of space configurations and their structure, measurement, transformations, and generalizations.",
            listOf(
                Quiz("What is 2 + 2?", listOf("1", "2", "3", "4"), 3),
                Quiz("What is 3 x 3?", listOf("6", "9", "12", "15"), 1),
                Quiz("What is the square root of 144?", listOf("14", "16", "18", "12"), 3),
                Quiz("What is the value of π (pi) to two decimal places?", listOf("3.16", "3.14", "3.20", "3.18"), 1)
            )
        ),
        Topic(
            "Physics",
            R.drawable.baseline_quiz_24,
            "Study of matter and energy.",
            "Physics is the natural science that studies matter, its fundamental constituents, its motion and behavior through space and time, and the related entities of energy and force.",
            listOf(
                Quiz(
                    "What is the speed of light?",
                    listOf("299,792,458 m/s", "100 m/s", "1,000,000 m/s", "10 m/s"),
                    0
                ),
                Quiz(
                    "What is Newton's first law of motion?",
                    listOf(
                        "An object at rest stays at rest",
                        "F=ma",
                        "For every action, there is an equal and opposite reaction",
                        "None of the above"
                    ),
                    0
                ),
                Quiz(
                    "What is the unit of measurement for electric current?",
                    listOf("Volt (V)", "Ohm (Ω)", "Ampere (A)", "Watt (W)"),
                    2
                ),
                Quiz(
                    "What is the SI unit of force?",
                    listOf("Newton (N)", "Joule (J)", "Watt (W)", "Volt (V)"),
                    0
                )
            )
        ),
        Topic(
            "Marvel Super Heroes",
            R.drawable.baseline_quiz_24,
            "Explore iconic Marvel characters",
            "Marvel Super Heroes are fictional characters with superhuman abilities, often portrayed as heroes who protect the world from powerful threats and villains.",
            listOf(
                Quiz("Who is Thor's brother?", listOf("Loki", "Iron Man", "Captain America", "Hulk"), 0),
                Quiz(
                    "What is Iron Man's real name?",
                    listOf("Tony Stark", "Steve Rogers", "Bruce Banner", "Peter Parker"),
                    0
                ),
                Quiz(
                    "What is the real name of Captain America?",
                    listOf("Steve Rogers", "Tony Stark", "Bruce Banner", "Peter Parker"),
                    0
                ),
                Quiz(
                    "Who is known as the 'Merc with a Mouth' in the Marvel Universe?",
                    listOf("Iron Man", "Deadpool", "Wolverine", "Thor"),
                    1
                )
            )
        )
    )

    override fun getTopics(): List<Topic> {
        return topics
    }
}

class QuizApp : Application() {
    private val topicRepository: TopicRepository = InMemoryTopicRepository()

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
    }
}
