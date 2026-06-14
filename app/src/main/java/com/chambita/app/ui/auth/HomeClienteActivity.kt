package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.cardview.widget.CardView
import com.chambita.app.R

class HomeClienteActivity : NavActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cliente)

        // Activar navegación inferior
        barraNavegacion()

        // Configurar botones de la cabecera
        findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener {
            showToast("Menú lateral próximamente")
        }

        findViewById<ImageButton>(R.id.btnNotificaciones)?.setOnClickListener {
            showToast("No tienes notificaciones nuevas")
        }

        findViewById<ImageView>(R.id.imgPerfil)?.setOnClickListener {
            // Reutilizamos la lógica de ir al perfil
            findViewById<android.view.View>(R.id.navPerfil)?.performClick()
        }

        // Configurar búsqueda y mapa (básico)
        findViewById<EditText>(R.id.etBuscar)?.setOnEditorActionListener { v, _, _ ->
            showToast("Buscando: ${v.text}")
            false
        }

        findViewById<CardView>(R.id.cardMapa)?.setOnClickListener {
            showToast("Abriendo mapa...")
        }
    }
}