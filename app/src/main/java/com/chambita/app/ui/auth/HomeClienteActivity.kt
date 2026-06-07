package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chambita.app.R

class HomeClienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cliente)

        val navInicio = findViewById<LinearLayout>(R.id.navInicio)
        val navTrabajos = findViewById<LinearLayout>(R.id.navTrabajos)
        val navMensajes = findViewById<LinearLayout>(R.id.navMensajes)
        val navPerfil = findViewById<LinearLayout>(R.id.navPerfil)

        navInicio.setOnClickListener {
            Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        }

        navTrabajos.setOnClickListener {
            Toast.makeText(this, "Sección de Trabajos próximamente", Toast.LENGTH_SHORT).show()
        }

        navMensajes.setOnClickListener {
            Toast.makeText(this, "Sección de Mensajes próximamente", Toast.LENGTH_SHORT).show()
        }

        navPerfil.setOnClickListener {
            Toast.makeText(this, "Perfil del Cliente próximamente", Toast.LENGTH_SHORT).show()
        }
    }
}