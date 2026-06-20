package com.chambita.app.ui.auth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilClienteActivity : NavActivity() {

    private val LOCATION_PERMISSION_REQ_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_cliente)

        barraNavegacion()
        cargarDatosUsuario()

        val swNotificaciones = findViewById<Switch>(R.id.switchNotificaciones)
        val swGps = findViewById<Switch>(R.id.switchGps)

        // --- LÓGICA DE NOTIFICACIONES ---
        swNotificaciones?.setOnCheckedChangeListener { _, isChecked ->
            actualizarPreferencia("notificacionesHabilitadas", isChecked)
        }

        // --- LÓGICA DE GPS / PERMISOS ---
        swGps?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                verificarPermisoUbicacion()
            }
        }

        findViewById<LinearLayout>(R.id.opEditarPerfil)?.setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opMetodosPago)?.setOnClickListener {
            startActivity(Intent(this, MetodosPagoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opDirecciones)?.setOnClickListener {
            startActivity(Intent(this, MisDireccionesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.opHistorialPagos)?.setOnClickListener {
            startActivity(Intent(this, HistorialPagosActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesion)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
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
                    findViewById<TextView>(R.id.txtNombre)?.text = document.getString("nombreCompleto") ?: "Usuario"
                    findViewById<TextView>(R.id.txtCorreo)?.text = document.getString("correo") ?: ""
                    
                    // Sincronizar estado del switch de notificaciones con la base de datos
                    val habilitadas = document.getBoolean("notificacionesHabilitadas") ?: false
                    findViewById<Switch>(R.id.switchNotificaciones)?.isChecked = habilitadas
                }
            }
    }

    private fun actualizarPreferencia(campo: String, valor: Any) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .update(campo, valor)
            .addOnFailureListener { e -> showToast("Error al guardar preferencia: ${e.message}") }
    }

    private fun verificarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE)
        } else {
            showToast("Permiso de ubicación ya concedido")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQ_CODE) {
            val swGps = findViewById<Switch>(R.id.switchGps)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Ubicación activada correctamente")
            } else {
                showToast("Permiso denegado. No se puede activar el GPS.")
                swGps?.isChecked = false // Desactivamos el switch si no dio permiso
            }
        }
    }
}
