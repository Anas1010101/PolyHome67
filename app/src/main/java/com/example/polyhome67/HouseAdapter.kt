package com.example.polyhome67

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class HouseAdapter(
    private val context: Context,
    private val houses: ArrayList<House>
) : BaseAdapter() {

    override fun getCount(): Int = houses.size

    override fun getItem(position: Int): Any = houses[position]

    override fun getItemId(position: Int): Long = houses[position].houseId.toLong()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_house, parent, false)

        val house = houses[position]

        val tvHouseId = view.findViewById<TextView>(R.id.tvHouseId)
        val tvTitleHouse = view.findViewById<TextView>(R.id.tvTitleHouse)
        val tvOwner = view.findViewById<TextView>(R.id.tvOwner)

        // Affichage cohérent avec l'API
        tvHouseId.text = "ID: ${house.houseId}"
        tvTitleHouse.text = "Maison ${house.houseId}"
        tvOwner.text = if (house.owner) "Propriétaire" else "Invité"

        return view
    }
}
