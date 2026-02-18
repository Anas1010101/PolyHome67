package com.example.polyhome67

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DevicesAdapter(
    private val context: Context,
    private val devices: ArrayList<Device>
) : BaseAdapter() {

    override fun getCount() = devices.size
    override fun getItem(position: Int) = devices[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val d = devices[position]
        val t1 = view.findViewById<TextView>(android.R.id.text1)
        val t2 = view.findViewById<TextView>(android.R.id.text2)

        // Ligne 1 : Type + ID
        t1.text = "${d.type} (#${d.id})"

        // Ligne 2 : commandes + états si présents
        val cmds = if (d.availableCommands.isEmpty()) "Aucune commande"
        else "Cmd: " + d.availableCommands.joinToString(", ")

        val states = buildString {
            if (d.opening != null) append(" | opening=${d.opening}")
            if (d.power != null) append(" | power=${d.power}")
        }

        t2.text = cmds + states

        return view
    }
}
