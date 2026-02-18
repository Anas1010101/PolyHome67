package com.example.polyhome67

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ScenariosActivity : AppCompatActivity() {

    private lateinit var token: String
    private var houseId: Int = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var progress: ProgressBar

    private val poltergeistRunning = AtomicBoolean(false)
    private var poltergeistThread: Thread? = null

    data class CommandRequest(val command: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scenarios)

        token = intent.getStringExtra("Token") ?: ""
        houseId = intent.getIntExtra("HouseId", -1)

        if (token.isBlank() || houseId == -1) {
            Toast.makeText(this, "Données manquantes (token/houseId)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        progress = findViewById(R.id.progressScenarios)

        findViewById<View>(R.id.cardCinema).setOnClickListener { runScenarioCinema() }
        findViewById<View>(R.id.cardDepart).setOnClickListener { runScenarioDepart() }
        findViewById<View>(R.id.cardReveil).setOnClickListener { runScenarioReveil() }
        findViewById<View>(R.id.cardPoltergeist).setOnClickListener { togglePoltergeist() }
    }

    override fun onStop() {
        super.onStop()
        stopPoltergeist()
    }

    private fun runScenarioCinema() {
        stopPoltergeist()
        setLoading(true)

        fetchDevices { devices ->
            Thread {
                devices.forEach { d ->
                    sendIfPossible(d, listOf("CLOSE", "SHUT", "DOWN"))
                    sendIfPossible(d, listOf("OFF", "TURN_OFF", "SWITCH_OFF"))
                }
                uiDone("Mode Cinéma appliqué")
            }.start()
        }
    }

    private fun runScenarioDepart() {
        stopPoltergeist()
        setLoading(true)

        fetchDevices { devices ->
            Thread {
                devices.forEach { d ->
                    sendIfPossible(d, listOf("OFF", "TURN_OFF", "SWITCH_OFF"))
                    sendIfPossible(d, listOf("CLOSE", "SHUT", "DOWN"))
                }
                uiDone("Mode Pas à la maison appliqué")
            }.start()
        }
    }

    private fun runScenarioReveil() {
        stopPoltergeist()
        setLoading(true)

        fetchDevices { devices ->
            Thread {
                val shutters = devices.filter { hasAnyCommand(it, listOf("OPEN", "UP")) && hasAnyCommand(it, listOf("CLOSE", "DOWN", "SHUT")) }
                val lights = devices.filter { hasAnyCommand(it, listOf("ON", "TURN_ON", "SWITCH_ON")) && hasAnyCommand(it, listOf("OFF", "TURN_OFF", "SWITCH_OFF")) }

                shutters.forEach { d -> sendIfPossible(d, listOf("OPEN", "UP")) }

                lights.forEachIndexed { idx, d ->
                    sendIfPossible(d, listOf("ON", "TURN_ON", "SWITCH_ON"))
                    if (idx < lights.lastIndex) Thread.sleep(200)
                }

                uiDone("Mode Réveil appliqué")
            }.start()
        }
    }

    private fun togglePoltergeist() {
        if (poltergeistRunning.get()) {
            stopPoltergeist()
            Toast.makeText(this, "Poltergeist arrêté", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        fetchDevices { devices ->
            val shutters = devices.filter { hasAnyCommand(it, listOf("OPEN", "UP")) && hasAnyCommand(it, listOf("CLOSE", "DOWN", "SHUT")) }
            val lights = devices.filter { hasAnyCommand(it, listOf("ON", "TURN_ON", "SWITCH_ON")) && hasAnyCommand(it, listOf("OFF", "TURN_OFF", "SWITCH_OFF")) }
            val garage = devices.filter { looksLikeGarage(it) && hasAnyCommand(it, listOf("OPEN")) && hasAnyCommand(it, listOf("CLOSE", "SHUT")) }

            if (shutters.isEmpty() && lights.isEmpty() && garage.isEmpty()) {
                uiDone("Aucun device compatible")
                return@fetchDevices
            }

            setLoading(false)
            Toast.makeText(this, "Poltergeist lancé (re-clique pour arrêter)", Toast.LENGTH_LONG).show()

            poltergeistRunning.set(true)

            poltergeistThread = Thread {
                var iSh = 0
                var iLi = 0
                var iGa = 0
                var step = 0

                while (poltergeistRunning.get()) {
                    val choose = step % 3

                    if (choose == 0 && shutters.isNotEmpty()) {
                        val d = shutters[iSh % shutters.size]
                        val cmd = if (step % 2 == 0) listOf("OPEN", "UP") else listOf("CLOSE", "DOWN", "SHUT")
                        sendIfPossible(d, cmd)
                        iSh++
                    } else if (choose == 1 && lights.isNotEmpty()) {
                        val d = lights[iLi % lights.size]
                        val cmd = if (step % 2 == 0) listOf("ON", "TURN_ON", "SWITCH_ON") else listOf("OFF", "TURN_OFF", "SWITCH_OFF")
                        sendIfPossible(d, cmd)
                        iLi++
                    } else if (garage.isNotEmpty()) {
                        val d = garage[iGa % garage.size]
                        val cmd = if (step % 2 == 0) listOf("OPEN") else listOf("CLOSE", "SHUT")
                        sendIfPossible(d, cmd)
                        iGa++
                    } else {
                        val any = (shutters + lights).takeIf { it.isNotEmpty() } ?: shutters + lights + garage
                        if (any.isNotEmpty()) {
                            val d = any[step % any.size]
                            sendIfPossible(d, listOf("STOP", "HALT"))
                        }
                    }

                    step++
                    Thread.sleep(550)
                }
            }.also { it.start() }
        }
    }

    private fun stopPoltergeist() {
        poltergeistRunning.set(false)
        poltergeistThread = null
    }

    private fun fetchDevices(onOk: (List<Device>) -> Unit) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices"
        Thread {
            try {
                Api().get<DevicesResponse>(url, { code, res ->
                    if (code == 200 && res != null) {
                        mainHandler.post { onOk(res.devices) }
                    } else {
                        uiDone("Erreur devices : $code")
                    }
                }, token)
            } catch (e: Exception) {
                uiDone("Erreur : ${e.message}")
            }
        }.start()
    }

    private fun hasAnyCommand(d: Device, aliases: List<String>): Boolean {
        val cmds = d.availableCommands.map { it.trim().uppercase() }
        return aliases.any { a -> cmds.any { it == a } }
    }

    private fun pickCommand(d: Device, aliases: List<String>): String? {
        val cmds = d.availableCommands.map { it.trim() }
        val upper = cmds.map { it.uppercase() }
        for (a in aliases) {
            val idx = upper.indexOf(a.uppercase())
            if (idx >= 0) return cmds[idx]
        }
        return null
    }

    private fun sendIfPossible(d: Device, aliases: List<String>) {
        val cmd = pickCommand(d, aliases) ?: return
        sendDeviceCommand(d.id, cmd)
        Thread.sleep(140)
    }

    private fun sendDeviceCommand(deviceId: String, command: String) {
        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/devices/$deviceId/command"
        try {
            Api().post<CommandRequest, Any?>(url, CommandRequest(command), { _, _ -> }, token)
        } catch (_: Exception) {
        }
    }

    private fun looksLikeGarage(d: Device): Boolean {
        val t = d.type.lowercase()
        val cmds = d.availableCommands.joinToString(",").lowercase()
        return t.contains("garage") || t.contains("door") || (cmds.contains("open") && cmds.contains("close") && d.opening == null && d.power == null)
    }

    private fun setLoading(isLoading: Boolean) {
        mainHandler.post { progress.visibility = if (isLoading) View.VISIBLE else View.GONE }
    }

    private fun uiDone(msg: String) {
        mainHandler.post {
            setLoading(false)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
