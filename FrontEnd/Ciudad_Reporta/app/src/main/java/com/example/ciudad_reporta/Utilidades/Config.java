package com.example.ciudad_reporta.Utilidades;

import android.content.SharedPreferences;
import android.content.Context;

public class Config {

    // Obtiene la IP del servidor guardada en SharedPreferences
    public static String getServerIp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        return prefs.getString("server_ip", "");
    }

    // Obtiene el ID del usuario guardado en SharedPreferences
    public static int getIdUsuario (Context context){
        SharedPreferences prefs = context.getSharedPreferences("usuario", Context.MODE_PRIVATE);
        return prefs.getInt("id_usuario",0);
    }

}