package com.example.graphvisualiser

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe

class SplashscreenActivity : AppCompatActivity() {
    // this is a separate viewmodel instance from the mainActivity one, used to validate the model has been downloaded
    private val myViewModel: MyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        downloadModel(myViewModel)     // download the model

        val loadingTextView = findViewById<TextView>(R.id.splashScreenLoadingStatusTextView)
        loadingTextView.text = "Downloading Machine Learning Model"

        val loadingLinearLayout = findViewById<LinearLayout>(R.id.splashScreenLoadingLinearLayout)
        val flashingAnimation = AlphaAnimation(0f, 1f)
        flashingAnimation.duration = 500
        flashingAnimation.startOffset = 1050
        flashingAnimation.repeatMode = Animation.REVERSE
        flashingAnimation.repeatCount = Animation.INFINITE
        loadingLinearLayout.startAnimation(flashingAnimation)

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

        myViewModel.modelPath.observe(this) {
            if (it != null) {   // model has been downloaded
                Handler(Looper.myLooper()!!).postDelayed({
                    val changedIntent = Intent(this, MainActivity::class.java)
                    changedIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(changedIntent)   // starting the activity should show no animation, otherwise it
                    finish()                // causes two separate animations to overlap with the override
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }, 2000)
            }
        }
    }
}