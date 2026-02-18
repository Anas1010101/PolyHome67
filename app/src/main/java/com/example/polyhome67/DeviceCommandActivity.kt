package com.example.polyhome67

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api

class DeviceCommandActivity : AppCompatActivity() {

    private var houseId: Int = -1
    private var deviceId: String = ""
    private lateinit var token: String

    private lateinit var tvTitle: TextView
    private lateinit var spCommands: Spinner
    private lateinit var btnSend: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvMsg: TextView

    private val commands: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_command)

        houseId = intent.getIntExtra("HouseId", -1)
        deviceId = intent.getStringExtra("DeviceId") ?: ""
        val deviceType = intent.getStringExtra("DeviceType") ?: "Device"
        val cmdArray = intent.getStringArrayListExtra("Commands") ?: arrayListOf()

        token = intent.getStringExtra("Token")
            ?: getSharedPreferences("polyhome_prefs", Context.MODE_PRIVATE)
                .getString("token", "") ?: ""

        if (houseId == -1 || deviceId.isBlank() || token.isBlank()) {
            Toast.makeText(this, "Données manquantes (token/houseId/deviceId)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvTitle = findViewById(R.id.tvDeviceTitle)
        spCommands = findViewById(R.id.spCommands)
        btnSend = findViewById(R.id.btnSendCommand)
        progress = findViewById(R.id.progressSendCommand)
        tvMsg = findViewById(R.id.tvCommandMsg)

        tvTitle.text = "$deviceType (#$deviceId)"

        commands.clear()
        commands.addAll(cmdArray)

        if (commands.isEmpty()) {
            commands.add("Aucune commande")
            btnSend.isEnabled = false
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, commands)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCommands.adapter = adapter

        btnSend.setOnClickListener { sendCommand() }
    }

    private fun sendCommand() {
        val command = spCommands.selectedItem?.toString()?.trim() ?: ""
        if (command.isBlank() || command == "Aucune commande") {
            showMsg("Commande invalide.", true)
            return
        }

        progress.visibility = View.VISIBLE
        btnSend.isEnabled = false
        tvMsg.visibility = View.GONE

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$deviceId/command"
        val body = DeviceCommandRequest(command = command)

        Thread {
            try {
                Api().post<DeviceCommandRequest>(
                    url,
                    body,
                    ::onResultSend,
                    token
                )
            } catch (e: Exception) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    btnSend.isEnabled = true
                    showMsg("Erreur (réseau/parsing) : ${e.message}", true)
                }
            }
        }.start()
    }

    private fun onResultSend(code: Int) {
        runOnUiThread {
            progress.visibility = View.GONE
            btnSend.isEnabled = true

            when (code) {
                200 -> showMsg("Commande envoyée ", false)
                403 -> showMsg("Accès interdit (token invalide / pas autorisé).", true)
                500 -> showMsg("Erreur serveur (500).", true)
                else -> showMsg("Erreur serveur : $code", true)
            }
        }
    }

    private fun showMsg(msg: String, isError: Boolean) {
        tvMsg.visibility = View.VISIBLE
        tvMsg.text = msg
        if (isError) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
