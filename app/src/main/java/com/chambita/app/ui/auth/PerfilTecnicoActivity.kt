package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chambita.app.R
import com.chambita.app.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class PerfilTecnicoActivity : NavActivity() {

    private val viewModel: PerfilViewModel by viewModels { AuthViewModelFactory(this) }
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_tecnico)

        barraNavegacion()
        cargarDatos()
        observarEstado()

        findViewById<LinearLayout>(R.id.btnCerrarSesion)?.setOnClickListener {
            viewModel.logout()
        }

        findViewById<LinearLayout>(R.id.cardEditarPerfil)?.setOnClickListener {
            startActivity(Intent(this, EditarPerfilTecnicoActivity::class.java))
        }
    }

    private fun cargarDatos() {
        if (uid == null) return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
            val usuario = doc.toObject(Usuario::class.java)
            usuario?.let {
                findViewById<TextView>(R.id.txtNombre).text = it.nombreCompleto
                findViewById<TextView>(R.id.txtCorreo).text = it.correo
                
                val img = findViewById<ImageView>(R.id.imgPerfil)
                if (it.fotoPerfil.isNotEmpty()) {
                    Glide.with(this).load(it.fotoPerfil).circleCrop().into(img)
                }
            }
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            viewModel.logoutEvent.collect {
                val intent = Intent(this@PerfilTecnicoActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
