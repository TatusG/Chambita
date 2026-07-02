package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.*
import com.chambita.app.R
import com.chambita.app.utils.Distritos
import com.chambita.app.data.local.AppDatabase
import com.chambita.app.data.local.entities.LocalAddressEntity
import com.chambita.app.models.Direccion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class AgregarDireccionActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var etAlias: EditText
    private lateinit var etDireccion: EditText
    private lateinit var spDistrito: Spinner
    private lateinit var etReferencia: EditText
    private lateinit var swPrincipal: Switch
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_direccion)

        inicializarComponentes()
        configurarSpinner()

        findViewById<TextView>(R.id.btnCerrar).setOnClickListener { finish() }
        btnGuardar.setOnClickListener { guardarDireccion() }
    }

    private fun inicializarComponentes() {
        etAlias = findViewById(R.id.etAlias)
        etDireccion = findViewById(R.id.etDireccion)
        spDistrito = findViewById(R.id.spDistrito)
        etReferencia = findViewById(R.id.etReferencia)
        swPrincipal = findViewById(R.id.swPrincipal)
        btnGuardar = findViewById(R.id.btnGuardar)
    }

    private fun configurarSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Distritos.listaLimaCallao)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spDistrito.adapter = adapter
    }

    private fun guardarDireccion() {
        if (uid == null) return

        val alias = etAlias.text.toString().trim()
        val direccionText = etDireccion.text.toString().trim()
        val distrito = spDistrito.selectedItem.toString()
        val referencia = etReferencia.text.toString().trim()
        val esPrincipal = swPrincipal.isChecked

        if (alias.isEmpty() || direccionText.isEmpty()) {
            showToast("Completa los campos obligatorios")
            return
        }

        val direccionId = db.collection("usuarios").document(uid).collection("direcciones").document().id
        
        val nuevaDireccion = Direccion(
            id = direccionId,
            alias = alias,
            direccion = direccionText,
            distrito = distrito,
            referencia = referencia,
            esPrincipal = esPrincipal,
            fechaRegistro = Timestamp.now()
        )

        btnGuardar.isEnabled = false
        
        // 1. Guardar en Firestore
        db.collection("usuarios").document(uid).collection("direcciones")
            .document(direccionId)
            .set(nuevaDireccion)
            .addOnSuccessListener {
                // 2. Guardar en Room
                val localEntity = LocalAddressEntity(
                    direccionId = direccionId,
                    clienteId = uid,
                    alias = alias,
                    direccion = direccionText,
                    distrito = distrito,
                    esPrincipal = esPrincipal
                )
                
                CoroutineScope(Dispatchers.IO).launch {
                    val localDao = AppDatabase.getDatabase(this@AgregarDireccionActivity).localAddressDao()
                    localDao.insertAddress(localEntity)
                    
                    runOnUiThread {
                        showToast("Dirección guardada")
                        finish()
                    }
                }
            }
            .addOnFailureListener {
                btnGuardar.isEnabled = true
                showToast("Error al guardar: ${it.message}")
            }
    }
}
