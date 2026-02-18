package com.example.polyhome67

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class HouseUsersAdapter(
    private val context: Context,
    private val users: ArrayList<HouseUser>
) : BaseAdapter() {

    override fun getCount() = users.size
    override fun getItem(position: Int) = users[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val u = users[position]
        val t1 = view.findViewById<TextView>(android.R.id.text1)
        val t2 = view.findViewById<TextView>(android.R.id.text2)

        t1.text = u.safeLogin()
        t2.text = if (u.isOwner()) "Propriétaire" else "Invité"

        return view
    }
}
