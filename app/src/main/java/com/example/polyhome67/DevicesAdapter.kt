package com.example.polyhome67

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class DevicesAdapter(
    private val context: Context,
    private val devices: ArrayList<Device>,
    private val onDeviceClick: (Device) -> Unit
) : BaseAdapter() {

    override fun getCount() = devices.size
    override fun getItem(position: Int) = devices[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_device, parent, false)

        val d = devices[position]

        val tvTitle = view.findViewById<TextView>(R.id.tvDeviceTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvDeviceSubtitle)
        val tvStatus = view.findViewById<TextView>(R.id.tvDeviceStatus)
        val tvDot = view.findViewById<TextView>(R.id.tvDeviceDot)

        tvTitle.text = prettyType(d.type)
        tvSubtitle.text = "ID : ${d.id}"

        val status = prettyStatus(d)
        tvStatus.text = status.text

        val dotColor = if (status.isActive) android.R.color.holo_purple else android.R.color.darker_gray
        tvDot.background?.setTint(ContextCompat.getColor(context, dotColor))

        // clic sur la carte
        view.setOnClickListener { onDeviceClick(d) }

        return view
    }

    data class StatusUi(val text: String, val isActive: Boolean)

    private fun prettyType(raw: String): String {
        return when (raw.trim().lowercase()) {
            "shutter" -> "Volet"
            "light" -> "Lumière"
            "heater" -> "Chauffage"
            "outlet", "plug", "socket" -> "Prise"
            else -> raw.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun prettyStatus(d: Device): StatusUi {

        val cmds = d.availableCommands.joinToString(",").lowercase()

        // ===== VOLETS (rolling shutter) =====
        d.opening?.let { pos ->
            return if (pos <= 0)
                StatusUi("Fermé", false)
            else
                StatusUi("Ouvert", true)
        }

        // ===== LUMIERES / PRISES / CHAUFFAGE =====
        d.power?.let { p ->
            return when (p) {
                0 -> StatusUi("Éteint", false)
                1 -> StatusUi("Allumé", true)   // API binaire
                else -> StatusUi("$p%", true)   // si jamais c’est un vrai %
            }
        }

        // ===== FALLBACK via commandes =====
        if (cmds.contains("open") || cmds.contains("close"))
            return StatusUi("Volet", true)

        if (cmds.contains("on") || cmds.contains("off"))
            return StatusUi("Interrupteur", true)

        return StatusUi("Inconnu", false)
    }


}
