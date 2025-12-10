package com.egeereyzee.multiapp

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope

class DevelopingActivity : AppCompatActivity() {
    private lateinit var textViewLoading: TextView
    private lateinit var rotateAnimation: Animation
    private var animationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaplayer)

//        initializeViews()
//        startLoadingAnimation()
    }

//    private fun initializeViews() {
//        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.an_rotate)
//        textViewLoading = findViewById(R.id.textViewWaiting)
//    }
//
//    private fun startLoadingAnimation() {
//        animationJob = lifecycleScope.launch {
//            textViewLoading.startAnimation(rotateAnimation)
//
//            val hourglass1 = getString(R.string.fa_hourglass_1)
//            val hourglass2 = getString(R.string.fa_hourglass_2)
//            val hourglass3 = getString(R.string.fa_hourglass_3)
//
//            val hourglasses = listOf(hourglass1, hourglass2, hourglass3)
//
//            var index = 0
//            while (isActive) {
//                textViewLoading.text = hourglasses[index]
//                if (index == 2) {
//                    textViewLoading.startAnimation(rotateAnimation)
//                    delay(500)
//                }
//                index = (index + 1) % hourglasses.size
//                delay(500)
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        animationJob?.cancel()
//        textViewLoading.clearAnimation()
//    }
}