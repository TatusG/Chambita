package com.chambita.app.ui.auth

import android.os.Bundle
import com.chambita.app.R

class MisSolicitudesActivity : NavActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_solicitudes)
        
        barraNavegacion()
    }
}