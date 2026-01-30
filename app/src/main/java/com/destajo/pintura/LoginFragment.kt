package com.destajo.pintura

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar si ya está logueado
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_login_to_dashboard)
            return
        }

        val inputEmail = view.findViewById<EditText>(R.id.inputEmail)
        val inputPassword = view.findViewById<EditText>(R.id.inputPassword)
        val btnEntrar = view.findViewById<Button>(R.id.btnLogin)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarLogin)

        progressBar.visibility = View.GONE

        btnEntrar.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty()) {
                inputEmail.error = "Campo requerido"
                inputEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                inputPassword.error = "Campo requerido"
                inputPassword.requestFocus()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnEntrar.isEnabled = false
            btnEntrar.text = "VERIFICANDO..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_login_to_dashboard)
                }
                .addOnFailureListener { exception ->
                    progressBar.visibility = View.GONE
                    btnEntrar.isEnabled = true
                    btnEntrar.text = "ENTRAR"

                    manejarErroresSeguros(exception)
                }
        }
    }

    // --- MANEJO DE ERRORES SEGURO ---
    private fun manejarErroresSeguros(exception: Exception) {
        val mensaje = when (exception) {

            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Usuario o contraseña incorrectos."

            // Error de conexión (este sí se puede decir)
            is FirebaseNetworkException -> "📡 Sin conexión a internet."

            // Cualquier otro error raro
            else -> " Error al iniciar sesión."
        }

        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        exception.printStackTrace()
    }
}