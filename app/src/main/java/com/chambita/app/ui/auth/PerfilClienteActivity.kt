package com.chambita.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilClienteActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_cliente)

        barraNavegacion()
        cargarDatosUsuario()

        findViewById<LinearLayout>(R.id.opEditarPerfil)?.setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opMetodosPago)?.setOnClickListener {
            showToast("Métodos de Pago próximamente")
        }

        findViewById<LinearLayout>(R.id.opDirecciones)?.setOnClickListener {
            showToast("Mis Direcciones próximamente")
        }

        findViewById<LinearLayout>(R.id.opHistorialPagos)?.setOnClickListener {
            showToast("Historial de Pagos próximamente")
        }

        // Cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesion)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            showToast("Sesión cerrada")
            // Redirigir al login al cerrar sesión
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun cargarDatosUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombreComplete") ?: "Usuario"
                    val correo = document.getString("correo") ?: ""

                    findViewById<TextView>(R.id.txtNombre)?.text = nombre
                    findViewById<TextView>(R.id.txtCorreo)?.text = correo
                }
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar datos: ${e.message}")
            }
    }
}
