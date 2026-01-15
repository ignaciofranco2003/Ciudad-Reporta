package com.example.ciudad_reporta.menu_principal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ciudad_reporta.login.MainActivity;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.menu_principal.acerca_app.AcercaDeLaAppActivity;
import com.example.ciudad_reporta.menu_principal.crear_reporte.CrearReporteActivity;
import com.example.ciudad_reporta.menu_principal.mis_reportes.MisReportesActivity;
import com.example.ciudad_reporta.menu_principal.reportes_cercanos.ReportesCercanosActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class MenuActivity extends AppCompatActivity {

    // Botones y variables de autenticación
    private Button btnLogout;
    private Button btnCrearReporte, btnMisReportes, btnVerReportesCercanos, btnAcercaApp;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Inicializar autenticación de Firebase
        mAuth = FirebaseAuth.getInstance();

        // Configura Google Sign-In para poder cerrar sesión correctamente
        mGoogleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
        );

        // Asignar los botones desde el layout
        btnLogout = findViewById(R.id.btnLogout);
        btnCrearReporte = findViewById(R.id.btnCrearReporte);
        btnMisReportes = findViewById(R.id.btnMisReportes);
        btnVerReportesCercanos = findViewById(R.id.btnVerReportesCercanos);
        btnAcercaApp = findViewById(R.id.btnAcercaApp);

        // Recuperar ID de usuario y rol desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        int idUsuario = prefs.getInt("id_usuario", -1);
        String correo = prefs.getString("correo", "sin correo");

        // Mostrar mensaje de bienvenida con el correo del usuario
        Toast.makeText(this, "Bienvenido: " + correo, Toast.LENGTH_LONG).show();

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(MenuActivity.this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que querés cerrar sesión?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // Cerrar sesión de Firebase
                        FirebaseAuth.getInstance().signOut();

                        // Cerrar sesión de Google
                        GoogleSignIn.getClient(
                                MenuActivity.this,
                                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .build()
                        ).signOut().addOnCompleteListener(task -> {
                            // Limpia los datos guardados localmente
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();

                            // Volver al login
                            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Ir a "Crear reporte"
        btnCrearReporte.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, CrearReporteActivity.class);
            startActivity(intent);
        });

        // Ir a "Mis reportes"
        btnMisReportes.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MisReportesActivity.class);
            startActivity(intent);
        });

        // Ir a "Reportes Cercanos"
        btnVerReportesCercanos.setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, ReportesCercanosActivity.class));
        });

        // Ir a "Acerca de la app"
        btnAcercaApp.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, AcercaDeLaAppActivity.class);
            startActivity(intent);
        });
    }
}