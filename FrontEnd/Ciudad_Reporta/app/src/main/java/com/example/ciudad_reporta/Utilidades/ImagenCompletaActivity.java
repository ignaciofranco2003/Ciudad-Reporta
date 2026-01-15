package com.example.ciudad_reporta.Utilidades;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;    // Librería para cargar imágenes desde URL
import com.example.ciudad_reporta.R;

public class ImagenCompletaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagen_completa);

        // Botón de volver
        ImageButton btnCerrar = findViewById(R.id.btnCerrar);
        btnCerrar.setOnClickListener(v -> finish());

        // ImageView donde mostrar la imagen
        ImageView imageView = findViewById(R.id.imgCompleta);

        // Obtener la URL de la imagen
        String url = getIntent().getStringExtra("imagenUrl");

        // Cargar la imagen usando Glide
        Glide.with(this).load(url).into(imageView);
    }
}
