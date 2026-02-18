package com.example.polyhome67

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.androidtp2.Api

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        val backLogin = findViewById<TextView>(R.id.tvBackLogin)
        backLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        createAccount()
    }

    public fun onSuccessRegister(responseCode: Int) {
        if (responseCode==200){
            val intent = Intent(
                this,
                MainActivity::class.java
            )
            startActivity(intent)
        }
    }


    public fun createAccount(){
        val nom = findViewById<EditText>(R.id.etRegisterLogin);
        val  password = findViewById<EditText>(R.id.etRegisterPassword);
        val  password2 = findViewById<EditText>(R.id.etRegisterConfirmPassword)


        val createAccount = findViewById<Button>(R.id.btnRegister);

        createAccount.setOnClickListener{
            val nomValue = nom.text.toString() ;
            val passwordValue =password.text.toString();
            val password2Value = password2.text.toString()

            if (password2Value == passwordValue){
                val dataRegister = DataRegister(
                    login = nomValue,
                    password=passwordValue
                )
                Api().post<DataRegister>("https://polyhome.lesmoulinsdudev.com/api/users/register", dataRegister, ::onSuccessRegister)

            }
            else{
                Toast.makeText(this, "Les mots de passes ne correspond pas.", Toast.LENGTH_SHORT).show()
            }


        }
    }
}