package com.example.ciudad_reporta.login;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.menu_principal.MenuActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    private String IP_SERVIDOR;

    private FirebaseAuth mAuth;
    private SignInButton signInButton;
    private TextView mTextViewRespuesta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        setContentView(com.example.ciudad_reporta.R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Solicitar permisos necesarios
        solicitarPermisosIniciales();

        // Obtener la IP del servidor guardada
        IP_SERVIDOR = Config.getServerIp(this);

        signInButton = findViewById(com.example.ciudad_reporta.R.id.btnGoogleSignIn);

        // Configurar opciones de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(com.example.ciudad_reporta.R.string.default_web_client_id)) // En strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Al hacer click en el botón de login, inicia sesión
        //signInButton.setOnClickListener(v -> signIn());
        signInButton.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                signIn(); // Esto muestra el diálogo de selección de cuenta + términos
            });
        });
        //signInButton.setOnClickListener(v -> mostrarDialogoInicio());

        // Botón config
        ImageButton configBtn = findViewById(R.id.btnConfig);
        configBtn.setOnClickListener(v -> {
            mostrarDialogoConfiguracion();
        });
    }

    // Lanzar Intent de login con Google
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
/*
    // Resultado de la actividad de login
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                mTextViewRespuesta.setText("Error: " + e.getMessage());
            }
        }
    }
*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // Aquí agregamos verificación previa
                    verificarCuentaEnBackend(account);
                }
            } catch (ApiException e) {
                mTextViewRespuesta.setText("Error: " + e.getMessage());
            }
        }
    }

    private void verificarCuentaEnBackend(GoogleSignInAccount account) {
        String email = account.getEmail();
        String url = "http://" + IP_SERVIDOR + ":5000/verificar_cuenta";

        RequestQueue queue = Volley.newRequestQueue(this);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        try {
                            boolean registrado = response.getBoolean("registrado");

                            if (!registrado) {
                                mostrarDialogoRegistrar(account); // Nuevo
                            }else{
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Error al procesar verificación", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "No se pudo verificar la cuenta", Toast.LENGTH_SHORT).show()
            );

            queue.add(request);
        } catch (Exception e) {
            Toast.makeText(this, "Error al enviar datos", Toast.LENGTH_SHORT).show();
        }
    }
    private void mostrarDialogoRegistrar(GoogleSignInAccount account) {
        new AlertDialog.Builder(this)
                .setTitle("Cuenta nueva")
                .setMessage("Esta cuenta no está registrada. ¿Deseás registrarte?")
                .setPositiveButton("Registrarme", (dialog, which) -> firebaseAuthWithGoogle(account.getIdToken()))
                .setNegativeButton("Cancelar", (dialog, which) -> mGoogleSignInClient.signOut())
                .show();
    }


    // Autenticación con Firebase
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            enviarEmailAlBackend(user.getEmail());
                        }
                    } else {
                        mTextViewRespuesta.setText("FirebaseAuth failed: " + task.getException());
                    }
                });
    }

    // Enviar email al backend y procesar la respuesta
    private void enviarEmailAlBackend(String email) {
        String url = "http://"+IP_SERVIDOR+":5000/login";
        RequestQueue queue = Volley.newRequestQueue(this);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("correo", email);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        try {
                            int id = response.getInt("id");
                            String rol = response.getString("rol");

                            guardarUsuario(id, email, rol);
                            irAlMenu();

                        } catch (Exception e) {
                            Toast.makeText(this, "Error al procesar respuesta del servidor", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                    }
            );

            queue.add(request);
        } catch (Exception e) {
            Toast.makeText(this, "Error al enviar datos al backend", Toast.LENGTH_SHORT).show();
        }
    }




    // Ir a la pantalla principal
    private void irAlMenu() {
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        startActivity(intent);
        finish();
    }

    // Si ya hay sesión iniciada, ir al backend directamente
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            enviarEmailAlBackend(currentUser.getEmail());
        }
    }

    // Mostrar un cuadro de diálogo para ingresar la IP del servidor
    private void mostrarDialogoConfiguracion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurar IP del servidor");

        final EditText input = new EditText(this);
        input.setHint("Ej: 192.168.0.105");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Creamos el AlertDialog
        AlertDialog dialog = builder.setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();

        // Al hacer clic en Guardar, valida y guarda la IP
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String ipIngresada = input.getText().toString().trim();

                if (!ipIngresada.isEmpty()) {
                    SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                    prefs.edit().putString("server_ip", ipIngresada).apply();
                    Toast.makeText(MainActivity.this, "IP guardada: " + ipIngresada, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "IP no válida", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Solicitar permisos de ubicación, cámara y almacenamiento
    private void solicitarPermisosIniciales() {
        List<String> permisosNecesarios = new ArrayList<>();

        // Verificar permisos uno por uno
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(android.Manifest.permission.CAMERA);
        }

        // Para Android 13 en adelante, permisos nuevos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Versiones anteriores
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Si hay permisos pendientes, solicitarlos
        if (!permisosNecesarios.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permisosNecesarios.toArray(new String[0]),
                    1002); // Código arbitrario para manejar respuesta
        }
    }

    // Guardar datos del usuario localmente
    private void guardarUsuario(int id, String correo, String rol) {
        SharedPreferences prefs = getSharedPreferences("usuario", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("id_usuario", id);
        editor.putString("correo", correo);
        editor.putString("rol", rol);
        editor.apply();
    }
}
