package com.example.my_project_1_aviv

class GameManager(private val lifeCount: Int = 3) {
    val ROWS = 5
    val COLS = 3

    var score: Int = 0
        private set

    var currentLives: Int = lifeCount
        private set

    val isGameOver: Boolean
        get() = currentLives <= 0

    fun reduceLives() {
        if (currentLives > 0) {
            currentLives--
        }
    }

    fun addScore(points: Int) {
        score += points
    }
}