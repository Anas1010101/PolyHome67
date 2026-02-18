package com.example.polyhome67

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtp2.Api

class HouseInfo : AppCompatActivity() {

    private lateinit var btnDevices: Button
    private lateinit var btnScenarios: Button

    private lateinit var token: String
    private var houseId: Int = -1
    private var isOwner: Boolean = false

    private lateinit var tvHouseTitle: TextView
    private lateinit var tvHouseStatus: TextView
    private lateinit var etUserLoginAccess: EditText
    private lateinit var btnGiveAccess: Button
    private lateinit var progressGiveAccess: ProgressBar
    private lateinit var btnRemoveAccess: Button
    private lateinit var progressRemoveAccess: ProgressBar
    private lateinit var tvGiveAccessMsg: TextView

    private lateinit var btnLoadUsers: Button
    private lateinit var progressUsers: ProgressBar
    private lateinit var listUsers: ListView
    private lateinit var usersAdapter: HouseUsersAdapter
    private val usersList = ArrayList<HouseUser>()

    private var usersVisible = false
    private var usersLoadedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_house_info)

        token = intent.getStringExtra("Token") ?: ""
        houseId = intent.getIntExtra("HouseId", -1)
        isOwner = intent.getBooleanExtra("Owner", false)

        if (token.isBlank() || houseId == -1) {
            Toast.makeText(this, "Données manquantes (token/houseId)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvHouseTitle = findViewById(R.id.tvHouseTitle)
        tvHouseStatus = findViewById(R.id.tvHouseStatus)
        etUserLoginAccess = findViewById(R.id.etUserLoginAccess)
        btnGiveAccess = findViewById(R.id.btnGiveAccess)
        progressGiveAccess = findViewById(R.id.progressGiveAccess)
        btnRemoveAccess = findViewById(R.id.btnRemoveAccess)
        progressRemoveAccess = findViewById(R.id.progressRemoveAccess)
        tvGiveAccessMsg = findViewById(R.id.tvGiveAccessMsg)

        btnLoadUsers = findViewById(R.id.btnLoadUsers)
        progressUsers = findViewById(R.id.progressUsers)
        listUsers = findViewById(R.id.listUsers)

        usersAdapter = HouseUsersAdapter(this, usersList)
        listUsers.adapter = usersAdapter

        btnDevices = findViewById(R.id.btnDevices)
        btnScenarios = findViewById(R.id.btnScenarios)

        tvHouseTitle.text = "Maison $houseId"
        tvHouseStatus.text = if (isOwner) "Statut : Propriétaire" else "Statut : Invité"

        if (!isOwner) {
            btnGiveAccess.isEnabled = false
            btnRemoveAccess.isEnabled = false
            tvGiveAccessMsg.visibility = View.VISIBLE
            tvGiveAccessMsg.text = "Seul le propriétaire peut gérer les accès."
        }

        setUsersVisible(false)

        btnGiveAccess.setOnClickListener { giveAccess() }
        btnRemoveAccess.setOnClickListener { removeAccess() }

        btnLoadUsers.setOnClickListener {
            if (usersVisible) {
                setUsersVisible(false)
            } else {
                setUsersVisible(true)
                if (!usersLoadedOnce) loadUsers() else usersAdapter.notifyDataSetChanged()
            }
        }

        btnDevices.setOnClickListener {
            val i = Intent(this, DevicesListActivity::class.java)
            i.putExtra("Token", token)
            i.putExtra("HouseId", houseId)
            i.putExtra("Owner", isOwner)
            startActivity(i)
        }

        btnScenarios.setOnClickListener {
            val i = Intent(this, ScenariosActivity::class.java)
            i.putExtra("Token", token)
            i.putExtra("HouseId", houseId)
            i.putExtra("Owner", isOwner)
            startActivity(i)
        }
    }

    private fun setUsersVisible(visible: Boolean) {
        usersVisible = visible
        listUsers.visibility = if (visible) View.VISIBLE else View.GONE
        progressUsers.visibility = View.GONE
        btnLoadUsers.text = if (visible) "Masquer les utilisateurs" else "Voir les utilisateurs"

        val lpDevices = btnDevices.layoutParams as ViewGroup.MarginLayoutParams
        lpDevices.topMargin = if (visible) dp(12) else 0
        btnDevices.layoutParams = lpDevices

        val lpScenarios = btnScenarios.layoutParams as ViewGroup.MarginLayoutParams
        lpScenarios.topMargin = dp(12)
        btnScenarios.layoutParams = lpScenarios
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun giveAccess() {
        val login = etUserLoginAccess.text.toString().trim()
        if (login.isBlank()) {
            showMsg("Veuillez saisir un login.", true)
            return
        }

        progressGiveAccess.visibility = View.VISIBLE
        btnGiveAccess.isEnabled = false
        btnRemoveAccess.isEnabled = false
        tvGiveAccessMsg.visibility = View.GONE

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val body = DataGiveAccess(userLogin = login)

        Thread {
            try {
                Api().post<DataGiveAccess, Any?>(url, body, ::onResultGiveAccess, token)
            } catch (e: Exception) {
                runOnUiThread {
                    progressGiveAccess.visibility = View.GONE
                    btnGiveAccess.isEnabled = isOwner
                    btnRemoveAccess.isEnabled = isOwner
                    showMsg("Erreur (réseau/parsing) : ${e.message}", true)
                }
            }
        }.start()
    }

    private fun onResultGiveAccess(code: Int, response: Any?) {
        runOnUiThread {
            progressGiveAccess.visibility = View.GONE
            btnGiveAccess.isEnabled = isOwner
            btnRemoveAccess.isEnabled = isOwner

            when (code) {
                200 -> {
                    showMsg("Accès accordé à l’utilisateur.", false)
                    usersLoadedOnce = false
                    if (usersVisible) loadUsers()
                }
                400 -> showMsg("Données incorrectes (login invalide).", true)
                403 -> showMsg("Accès interdit (token invalide ou pas propriétaire).", true)
                409 -> showMsg("Cet utilisateur est déjà associé à la maison.", true)
                else -> showMsg("Erreur serveur : $code", true)
            }
        }
    }

    private fun removeAccess() {
        val login = etUserLoginAccess.text.toString().trim()
        if (login.isBlank()) {
            showMsg("Veuillez saisir un login.", true)
            return
        }

        progressRemoveAccess.visibility = View.VISIBLE
        btnGiveAccess.isEnabled = false
        btnRemoveAccess.isEnabled = false
        tvGiveAccessMsg.visibility = View.GONE

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"
        val body = DataGiveAccess(userLogin = login)

        Thread {
            try {
                Api().delete<DataGiveAccess>(url, body, { code -> onResultRemoveAccess(code, null) }, token)
            } catch (e: Exception) {
                runOnUiThread {
                    progressRemoveAccess.visibility = View.GONE
                    btnGiveAccess.isEnabled = isOwner
                    btnRemoveAccess.isEnabled = isOwner
                    showMsg("Erreur (réseau/parsing) : ${e.message}", true)
                }
            }
        }.start()
    }

    private fun onResultRemoveAccess(code: Int, response: Any?) {
        runOnUiThread {
            progressRemoveAccess.visibility = View.GONE
            btnGiveAccess.isEnabled = isOwner
            btnRemoveAccess.isEnabled = isOwner

            when (code) {
                200 -> {
                    showMsg("Accès retiré avec succès.", false)
                    usersLoadedOnce = false
                    if (usersVisible) loadUsers()
                }
                400 -> showMsg("Requête invalide.", true)
                403 -> showMsg("Action interdite (pas propriétaire / token invalide).", true)
                404 -> showMsg("Utilisateur non associé à cette maison.", true)
                else -> showMsg("Erreur serveur : $code", true)
            }
        }
    }

    private fun loadUsers() {
        progressUsers.visibility = View.VISIBLE
        tvGiveAccessMsg.visibility = View.GONE

        val url = "https://polyhome.lesmoulinsdudev.com/api/houses/$houseId/users"

        Thread {
            try {
                Api().get<Array<HouseUser>>(url, ::onResultUsers, token)
            } catch (e: Exception) {
                runOnUiThread {
                    progressUsers.visibility = View.GONE
                    showMsg("Erreur (réseau/parsing) : ${e.message}", true)
                }
            }
        }.start()
    }

    private fun onResultUsers(code: Int, response: Array<HouseUser>?) {
        runOnUiThread {
            progressUsers.visibility = View.GONE

            when (code) {
                200 -> {
                    usersLoadedOnce = true
                    usersList.clear()
                    if (response != null) usersList.addAll(response)
                    usersAdapter.notifyDataSetChanged()
                    if (usersList.isEmpty()) showMsg("Aucun utilisateur associé.", false)
                }
                400 -> showMsg("Données incorrectes.", true)
                403 -> showMsg("Accès interdit (token invalide ou non membre de la maison).", true)
                else -> showMsg("Erreur serveur : $code", true)
            }
        }
    }

    private fun showMsg(msg: String, isError: Boolean) {
        tvGiveAccessMsg.visibility = View.VISIBLE
        tvGiveAccessMsg.text = msg
        if (isError) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
