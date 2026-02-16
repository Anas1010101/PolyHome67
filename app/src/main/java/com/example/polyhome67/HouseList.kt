package com.example.polyhome67

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class HousesList : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_houses_list)

        val token = intent.getStringExtra("Token")
        println("TOKEN REÇU DANS HOUSES: $token")
    }
}
