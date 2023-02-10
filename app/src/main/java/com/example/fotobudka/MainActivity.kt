package com.example.fotobudka

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fotobudka.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraFragment = CameraFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, cameraFragment)
            .commit()
    }
}