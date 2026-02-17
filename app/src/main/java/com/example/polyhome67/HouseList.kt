package com.example.polyhome67

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api

class HousesList : AppCompatActivity() {

    private lateinit var token: String
    private lateinit var houseList: ArrayList<House>
    private lateinit var listView: ListView
    private lateinit var adapter: HouseAdapter
    private lateinit var tvError: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_houses_list)

        token = intent.getStringExtra("Token") ?: ""
        if (token.isBlank()) {
            Toast.makeText(this, "Token manquant", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        houseList = ArrayList()
        listView = findViewById(R.id.listHouses)
        tvError = findViewById(R.id.tvHousesError)
        progress = findViewById(R.id.progressHouses)

        adapter = HouseAdapter(this, houseList)
        listView.adapter = adapter

        showHouses()
    }

    fun onSuccessShowHouses(responseCode: Int, houses: Array<House>?) {
        Log.d("HOUSES_API", "code=$responseCode houses=${houses?.size}")

        runOnUiThread {
            progress.visibility = ProgressBar.GONE

            if (responseCode == 200 && houses != null) {
                houseList.clear()
                houseList.addAll(houses)
                adapter.notifyDataSetChanged()

                if (houseList.isEmpty()) {
                    tvError.text = "Aucune maison trouvée pour ce compte."
                    tvError.visibility = TextView.VISIBLE
                } else {
                    tvError.visibility = TextView.GONE
                }
            } else {
                tvError.text = "Erreur API : $responseCode"
                tvError.visibility = TextView.VISIBLE
            }
        }
    }

    private fun showHouses() {
        progress.visibility = ProgressBar.VISIBLE
        tvError.visibility = TextView.GONE

        // DEBUG TOKEN
        Log.d("TOKEN_DEBUG", token)

        Api().get<Array<House>>(
            "https://polyhome.lesmoulinsdudev.com/api/houses",
            ::onSuccessShowHouses,
            token
        )
    }
}
