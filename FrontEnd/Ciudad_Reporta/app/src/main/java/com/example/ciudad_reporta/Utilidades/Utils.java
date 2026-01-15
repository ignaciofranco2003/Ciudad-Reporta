package com.example.ciudad_reporta.Utilidades;

import android.content.Context;
import android.content.Intent;

public class Utils {

    public static void abrirImagenPantallaCompleta(Context context, String urlImagenCompleta) {
        // Intent para abrir la actividad de imagen completa
        Intent intent = new Intent(context, ImagenCompletaActivity.class);
        intent.putExtra("imagenUrl", urlImagenCompleta);
        context.startActivity(intent);
    }

}