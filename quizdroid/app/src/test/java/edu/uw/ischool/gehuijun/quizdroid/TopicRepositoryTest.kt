package edu.uw.ischool.gehuijun.quizdroid

import org.junit.Assert.*
import org.junit.Test

class TopicRepositoryTest {

    private val topicRepository: TopicRepository = InMemoryTopicRepository()

    @Test
    fun testGetTopics() {
        val topics = topicRepository.getTopics()
        assertNotNull(topics)
        assertTrue(topics.isNotEmpty())
    }

    @Test
    fun testGetTopicByTitle() {
        val topics = topicRepository.getTopics()
        val firstTopic = topics.firstOrNull()

        assertNotNull(firstTopic)

        val title = firstTopic!!.title
        val retrievedTopic = topics.find { it.title == title }

        assertNotNull(retrievedTopic)
        assertEquals(firstTopic, retrievedTopic)
    }

    @Test
    fun testGetQuizByTitle() {
        val topics = topicRepository.getTopics()
        val firstTopic = topics.firstOrNull()

        assertNotNull(firstTopic)

        val quizzes = firstTopic!!.quizzes
        val firstQuiz = quizzes.firstOrNull()

        assertNotNull(firstQuiz)

        val questionText = firstQuiz!!.questionText
        val retrievedQuiz = quizzes.find { it.questionText == questionText }

        assertNotNull(retrievedQuiz)
        assertEquals(firstQuiz, retrievedQuiz)
    }

    @Test
    fun getTopics_returnsNonEmptyList() {
        val repository = InMemoryTopicRepository()
        val topics = repository.getTopics()
        assert(topics.isNotEmpty())
    }

    @Test
    fun getTopics_returnsCorrectNumberOfQuizzes() {
        val repository = InMemoryTopicRepository()
        val topics = repository.getTopics()

        // Assuming that there are 4 quizzes in each topic (as in the provided example)
        val expectedTotalQuizzes = 4 * topics.size

        val actualTotalQuizzes = topics.sumOf { it.quizzes.size }
        assertEquals(expectedTotalQuizzes, actualTotalQuizzes)
    }

    @Test
    fun getTopics_returnsCorrectQuizData() {
        val repository = InMemoryTopicRepository()
        val topics = repository.getTopics()

        val expectedQuiz = Quiz("What is 2 + 2?", listOf("1", "2", "3", "4"), 3)
        val actualQuiz = topics[0].quizzes[0] // Assuming first topic and first quiz

        assertEquals(expectedQuiz.questionText, actualQuiz.questionText)
        assertEquals(expectedQuiz.choices, actualQuiz.choices)
        assertEquals(expectedQuiz.correctAnswer, actualQuiz.correctAnswer)
    }
}
