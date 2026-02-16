package com.example.polyhome67

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Register()
        login()
    }

    fun Register() {
        val textRegister = findViewById<TextView>(R.id.tvGoRegister)
        textRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 🔹 Callback API renommé
    fun resultConnexion(responseCode: Int, response: AuthResponse?) {
        runOnUiThread {
            when (responseCode) {
                200 -> {
                    println("TOKEN REÇU LOGIN: ${response?.token}")
                    if (response?.token != null) {
                        val intent = Intent(this, HousesList::class.java)
                        intent.putExtra("Token", response.token)
                        startActivity(intent)
                    }
                }
                404 -> Toast.makeText(this, "Les identifiants sont incorrects", Toast.LENGTH_LONG).show()
                400 -> Toast.makeText(this, "Champs invalides", Toast.LENGTH_LONG).show()
                else -> Toast.makeText(this, "Erreur du serveur : $responseCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun login() {
        val login = findViewById<EditText>(R.id.etLogin)
        val password = findViewById<EditText>(R.id.etPassword)
        val connexion = findViewById<Button>(R.id.btnLogin)

        connexion.setOnClickListener {

            val dataConnexion = DataConnexion(
                login = login.text.toString(),
                password = password.text.toString()
            )

            Api().post<DataConnexion, AuthResponse>(
                "https://polyhome.lesmoulinsdudev.com/api/users/auth",
                dataConnexion,
                ::resultConnexion
            )
        }
    }
}
