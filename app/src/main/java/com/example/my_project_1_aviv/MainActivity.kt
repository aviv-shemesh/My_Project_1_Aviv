package com.example.my_project_1_aviv

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var imgPlayer: ImageView
    private lateinit var berets: Array<ImageView>
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var hearts: Array<ImageView>

    private lateinit var gameManager: GameManager

    private val handler = Handler(Looper.getMainLooper())
    private var gameRunning = false
    private var laneOffset = 0f
    private var currentLane = 1

    private var beretSpeed = 120f
    private val BERET_GAP = 350f
    private val SPAWN_Y = 300f

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!gameRunning) return
            moveBerets()
            checkCollisions()
            handler.postDelayed(this, Constants.Timer.DELAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SignalManager.init(this)

        initViews()

        imgPlayer.doOnLayout {
            initGameDimensions()
            startGame()
        }
        initButtons()
    }

    private fun initViews() {
        imgPlayer = findViewById(R.id.imgPlayer)
        buttonsLayout = findViewById(R.id.buttonsLayout)

        // --- הוספת 9 כומתות למערך ---
        berets = arrayOf(
            findViewById(R.id.imgBeret1),
            findViewById(R.id.imgBeret2),
            findViewById(R.id.imgBeret3),
            findViewById(R.id.imgBeret4),
            findViewById(R.id.imgBeret5),
            findViewById(R.id.imgBeret6),
            findViewById(R.id.imgBeret7),
            findViewById(R.id.imgBeret8),
            findViewById(R.id.imgBeret9)
        )
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)

        hearts = arrayOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )
    }

    private fun initGameDimensions() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        laneOffset = screenWidth / 3f
        currentLane = 1
        imgPlayer.translationX = 0f
    }

    private fun initButtons() {
        btnLeft.setOnClickListener {
            if (!gameRunning) return@setOnClickListener
            if (currentLane > 0) {
                currentLane--
                movePlayerToLane()
            }
        }
        btnRight.setOnClickListener {
            if (!gameRunning) return@setOnClickListener
            if (currentLane < 2) {
                currentLane++
                movePlayerToLane()
            }
        }
    }

    private fun movePlayerToLane() {
        val targetX = when (currentLane) {
            0 -> -laneOffset
            1 -> 0f
            2 -> laneOffset
            else -> 0f
        }
        imgPlayer.animate()
            .translationX(targetX)
            .setDuration(100)
            .start()
    }

    private fun startGame() {
        gameManager = GameManager(hearts.size)
        gameRunning = true
        updateUi()

        val startY = SPAWN_Y
        for (i in berets.indices) {
            berets[i].y = startY - (i * BERET_GAP)
            randomizeLane(berets[i])
            if (berets[i].y < SPAWN_Y) {
                berets[i].alpha = 0f
            } else {
                berets[i].alpha = 1f
            }
        }
        handler.post(gameLoop)
    }

    private fun moveBerets() {
        val totalChainHeight = berets.size * BERET_GAP
        val limitY = buttonsLayout.y

        for (beret in berets) {
            beret.y += beretSpeed

            if (beret.y < SPAWN_Y) {
                beret.alpha = 0f
            } else {
                beret.alpha = 1f
            }

            if (beret.y + beret.height >= limitY) {
                gameManager.addScore(10)
                beret.y -= totalChainHeight
                randomizeLane(beret)
            }
        }
    }

    private fun randomizeLane(beret: ImageView) {
        val randomLane = Random.nextInt(0, 3)
        val targetX = when (randomLane) {
            0 -> -laneOffset
            1 -> 0f
            2 -> laneOffset
            else -> 0f
        }
        beret.translationX = targetX
    }

    private fun checkCollisions() {
        val playerRect = Rect()
        imgPlayer.getGlobalVisibleRect(playerRect)
        playerRect.inset(40, 40)

        for (beret in berets) {
            if (beret.alpha == 1f) {
                val beretRect = Rect()
                beret.getGlobalVisibleRect(beretRect)
                beretRect.inset(30, 30)

                if (Rect.intersects(playerRect, beretRect)) {
                    handleCrash(beret)
                }
            }
        }
    }

    private fun handleCrash(hitBeret: ImageView) {
        gameManager.reduceLives()
        updateUi()

        SignalManager.getInstance().toast("Hit!")
        SignalManager.getInstance().vibrate()

        if (gameManager.isGameOver) {
            changeActivity("GAME OVER", gameManager.score)
        } else {
            hitBeret.translationX = 3000f
        }
    }

    private fun changeActivity(message: String, score: Int) {
        gameRunning = false
        val intent = Intent(this, ScoreActivity::class.java)
        val bundle = Bundle()
        bundle.putString(Constants.STATUS_KEY, message)
        bundle.putInt(Constants.SCORE_KEY, score)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    private fun updateUi() {
        for (i in hearts.indices) {
            if (i < gameManager.currentLives) {
                hearts[i].visibility = View.VISIBLE
            } else {
                hearts[i].visibility = View.INVISIBLE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        gameRunning = false
    }

    override fun onResume() {
        super.onResume()
        if (::gameManager.isInitialized && !gameManager.isGameOver) {
            gameRunning = true
            handler.post(gameLoop)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameLoop)
    }
}