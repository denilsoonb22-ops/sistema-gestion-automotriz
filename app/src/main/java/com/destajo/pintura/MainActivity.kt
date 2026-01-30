package com.destajo.pintura

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()

    // Auto-Logout (5 minutos)
    private val handler = Handler(Looper.getMainLooper())
    private val TIEMPO_LIMITE = 5 * 60 * 1000L
    private val cerrarSesionRunnable = Runnable { cerrarSesionPorInactividad() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // Elementos de la barra superior global
        val topBar = findViewById<View>(R.id.globalTopBar)
        val btnSalir = findViewById<ImageButton>(R.id.globalBtnSalir)
        val lblPantalla = findViewById<TextView>(R.id.globalLblPantalla)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_login) {
                bottomNav.visibility = View.GONE
                topBar.visibility = View.GONE
                stopDisconnectTimer()
            } else {
                bottomNav.visibility = View.VISIBLE
                topBar.visibility = View.VISIBLE
                resetDisconnectTimer()

                // Títulos dinámicos
                lblPantalla.text = when(destination.id) {
                    R.id.nav_lista -> "TRABAJOS ACTIVOS"
                    R.id.nav_agregar -> "GESTIÓN Y EDICIÓN"
                    else -> "PINTURA"
                }
            }
        }

        btnSalir.setOnClickListener { confirmarSalida() }
    }

    // --- LÓGICA DE SEGURIDAD ---
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        resetDisconnectTimer()
        return super.dispatchTouchEvent(ev)
    }

    private fun resetDisconnectTimer() {
        if (navController.currentDestination?.id == R.id.nav_login) return
        handler.removeCallbacks(cerrarSesionRunnable)
        handler.postDelayed(cerrarSesionRunnable, TIEMPO_LIMITE)
    }

    private fun stopDisconnectTimer() {
        handler.removeCallbacks(cerrarSesionRunnable)
    }

    private fun cerrarSesionPorInactividad() {
        if (auth.currentUser != null) {
            Toast.makeText(this, "Sesión cerrada por inactividad", Toast.LENGTH_LONG).show()
            realizarLogout()
        }
    }

    private fun confirmarSalida() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Deseas salir del sistema?")
            .setPositiveButton("SALIR") { _, _ -> realizarLogout() }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun realizarLogout() {
        stopDisconnectTimer()
        auth.signOut()
        navController.navigate(R.id.nav_login)
    }
}