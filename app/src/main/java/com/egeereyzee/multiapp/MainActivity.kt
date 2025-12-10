package com.egeereyzee.multiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.egeereyzee.multiapp.CalculatorActivity
import android.view.animation.AnimationUtils
import android.view.animation.Animation

class MainActivity : AppCompatActivity() {
    private lateinit var buttonSettings: Button
    private lateinit var buttonCalculator: Button
    private lateinit var buttonMediaPlayer: Button
    private lateinit var buttonLocator: Button
    private lateinit var buttonNetwork: Button
    private lateinit var buttonClient: Button
    private lateinit var buttonSocket: Button
    private lateinit var rotateAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupButtons()
    }

    private fun initializeViews() {
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.an_rotate)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonCalculator = findViewById(R.id.buttonCalculator)
        buttonMediaPlayer = findViewById(R.id.buttonMediaPlayer)
        buttonLocator = findViewById(R.id.buttonLocator)
        buttonNetwork = findViewById(R.id.buttonNetwork)
        buttonClient = findViewById(R.id.buttonClient)
        buttonSocket = findViewById(R.id.buttonSocket)
    }

    private fun setupButtons() {
        buttonSettings.setOnClickListener {
            buttonSettings.startAnimation(rotateAnimation)
        }

        buttonCalculator.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        buttonMediaPlayer.setOnClickListener {
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
        }

        buttonLocator.setOnClickListener {
            val intent = Intent(this, DevelopingActivity::class.java)
            startActivity(intent)
        }

        buttonNetwork.setOnClickListener {
            val intent = Intent(this, DevelopingActivity::class.java)
            startActivity(intent)
        }

        buttonClient.setOnClickListener {
            val intent = Intent(this, DevelopingActivity::class.java)
            startActivity(intent)
        }

        buttonSocket.setOnClickListener {
            val intent = Intent(this, DevelopingActivity::class.java)
            startActivity(intent)
        }
    }
}