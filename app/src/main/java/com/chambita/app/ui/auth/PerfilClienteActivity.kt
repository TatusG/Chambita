package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.chambita.app.R
import com.google.firebase.auth.FirebaseAuth

class PerfilClienteActivity : NavActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_cliente)

        barraNavegacion()

        findViewById<LinearLayout>(R.id.opEditarPerfil)?.setOnClickListener {
            showToast("Editar Perfil próximamente")
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
            finish()
        }
    }
}