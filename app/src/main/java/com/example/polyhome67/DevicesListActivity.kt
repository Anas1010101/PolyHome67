package com.example.polyhome67

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api

class DevicesListActivity : AppCompatActivity() {

    private lateinit var token: String
    private var houseId: Int = -1

    private lateinit var listView: ListView
    private lateinit var tvError: TextView
    private lateinit var progress: ProgressBar

    private lateinit var tvDashTotal: TextView
    private lateinit var tvDashActive: TextView
    private lateinit var tvDashPercent: TextView
    private lateinit var tvDashExtra: TextView
    private lateinit var pbActive: ProgressBar

    private val devicesList = ArrayList<Device>()
    private lateinit var adapter: DevicesAdapter

    private val deviceCommandLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            loadDevices()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_devices_list)

        token = intent.getStringExtra("Token") ?: ""
        houseId = intent.getIntExtra("HouseId", -1)

        if (token.isBlank() || houseId == -1) {
            Toast.makeText(this, "Données manquantes (token/houseId)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        listView = findViewById(R.id.listDevices)
        tvError = findViewById(R.id.tvDevicesError)
        progress = findViewById(R.id.progressDevices)

        tvDashTotal = findViewById(R.id.tvDashTotal)
        tvDashActive = findViewById(R.id.tvDashActive)
        tvDashPercent = findViewById(R.id.tvDashPercent)
        tvDashExtra = findViewById(R.id.tvDashExtra)
        pbActive = findViewById(R.id.pbActive)

        adapter = DevicesAdapter(this, devicesList) { d ->
            val intent = Intent(this, DeviceCommandActivity::class.java)
            intent.putExtra("Token", token)
            intent.putExtra("HouseId", houseId)
            intent.putExtra("DeviceId", d.id)
            intent.putExtra("DeviceType", d.type)
            intent.putStringArrayListExtra("Commands", ArrayList(d.availableCommands))
            deviceCommandLauncher.launch(intent)
        }
        listView.adapter = adapter

        loadDevices()
    }

    private fun loadDevices() {
        progress.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"

        Thread {
            try {
                Api().get<DevicesResponse>(url, ::onResultDevices, token)
            } catch (e: Exception) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Erreur (réseau/parsing) : ${e.message}"
                }
            }
        }.start()
    }

    private fun onResultDevices(code: Int, response: DevicesResponse?) {
        Log.d("DEVICES_API", "code=$code size=${response?.devices?.size}")

        runOnUiThread {
            progress.visibility = View.GONE

            when (code) {
                200 -> {
                    devicesList.clear()
                    response?.devices?.let { devicesList.addAll(it) }
                    adapter.notifyDataSetChanged()

                    val stats = DashboardUtils.compute(devicesList)
                    tvDashTotal.text = "Total : ${stats.total}"
                    tvDashActive.text = "Actifs : ${stats.active}"
                    tvDashPercent.text = "${stats.percentActive}%"
                    pbActive.progress = stats.percentActive
                    tvDashExtra.text = "Ouverts : ${stats.opened} | Allumés : ${stats.powered}"

                    if (devicesList.isEmpty()) {
                        tvError.visibility = View.VISIBLE
                        tvError.text = "Aucun périphérique trouvé."
                    } else {
                        tvError.visibility = View.GONE
                    }
                }

                403 -> {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Accès interdit (token invalide / pas membre de la maison)."
                }

                404 -> {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Maison introuvable."
                }

                else -> {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Erreur serveur : $code"
                }
            }
        }
    }
}
