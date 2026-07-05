package com.chambita.app.ui.auth

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.chambita.app.R
import com.chambita.app.models.Pago
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DashboardGananciasActivity : NavActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_ganancias)

        barraNavegacion()
        
        findViewById<ImageView>(R.id.btnVolver)?.setOnClickListener { finish() }

        actualizarFechaHoy()
        cargarDatosGanancias()
    }

    private fun actualizarFechaHoy() {
        val sdf = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE"))
        findViewById<TextView>(R.id.txtFechaHoy)?.text = sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun cargarDatosGanancias() {
        if (uid == null) return

        db.collection("pagos")
            .whereEqualTo("tecnicoId", uid)
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val pagos = snapshot.toObjects(Pago::class.java)
                calcularYMostrarMetricas(pagos)
            }
    }

    private fun calcularYMostrarMetricas(pagos: List<Pago>) {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        var gananciaHoy = 0.0
        var gananciaMes = 0.0
        var conteoServiciosMes = 0

        pagos.forEach { pago ->
            val fecha = pago.fechaRegistro?.toDate() ?: return@forEach
            
            if (fecha.after(hoy)) {
                gananciaHoy += pago.monto
            }
            
            if (fecha.after(inicioMes)) {
                gananciaMes += pago.monto
                conteoServiciosMes++
            }
        }

        // Formato con punto (Locale.US)
        findViewById<TextView>(R.id.txtGananciaHoy)?.text = String.format(Locale.US, "S/ %.2f", gananciaHoy)
        findViewById<TextView>(R.id.txtGananciaMes)?.text = String.format(Locale.US, "S/ %.2f", gananciaMes)
        findViewById<TextView>(R.id.txtConteoServicios)?.text = conteoServiciosMes.toString()
    }
}
