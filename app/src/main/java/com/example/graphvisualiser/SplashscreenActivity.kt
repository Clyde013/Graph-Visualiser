package com.example.graphvisualiser

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class SplashscreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        downloadModel(null)     // just download the model

        val splashScreenLayout = findViewById<LinearLayout>(R.id.splashScreenLinearLayout)
        // start objects as invisible
        splashScreenLayout.scaleX = 0f
        splashScreenLayout.scaleY = 0f

        // fancy animations to pop up the splashscreen
        val scaleXAnimator = ObjectAnimator.ofFloat(splashScreenLayout, "scaleX", 1.0f)
        scaleXAnimator.interpolator = AnticipateOvershootInterpolator()
        val scaleYAnimator = ObjectAnimator.ofFloat(splashScreenLayout, "scaleY", 1.0f)
        scaleYAnimator.interpolator = AnticipateOvershootInterpolator()
        val scaleAnimatorSet = AnimatorSet()
        scaleAnimatorSet.playTogether(scaleXAnimator, scaleYAnimator)
        scaleAnimatorSet.duration = 1000
        scaleAnimatorSet.startDelay = 50
        scaleAnimatorSet.start()

        Handler(Looper.myLooper()!!).postDelayed({
            val changedIntent = Intent(this, MainActivity::class.java)
            changedIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(changedIntent)   // starting the activity should show no animation, otherwise it
            finish()                // causes two separate animations to overlap with the override
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 2000)
    }
}