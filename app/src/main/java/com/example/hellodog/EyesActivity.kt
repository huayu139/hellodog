package com.example.hellodog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class EyesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eyes)
        // LottieAnimationView will auto-play and loop as defined in XML
        val animationView = findViewById<LottieAnimationView>(R.id.eyeAnimation)
        // Ensure the animation resource is loaded (raw/eyes_animation.json)
        animationView.setAnimation(R.raw.eyes_animation)
        animationView.playAnimation()
    }
}
