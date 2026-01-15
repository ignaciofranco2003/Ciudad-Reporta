package com.example.ciudad_reporta.menu_principal.mis_reportes;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.Utilidades.Utils;

import java.util.List;

public class ReporteAdapter extends ArrayAdapter<Reporte> {

    // Manejar acciones (editar, borrar, marcar como solucionado) desde la actividad que use el adaptador
    public interface OnReporteActionListener {
        void onEditarReporte(int idReporte);
        void onBorrarReporte(int idReporte);
        void onMarcarComoSolucionado(int idReporte);
    }

    private OnReporteActionListener listener;   // Listener para acciones de usuario
    private String IP_SERVICIO;                 // IP del servidor para cargar imágenes

    // Constructor que recibe contexto, lista de reportes y listener de acciones
    public ReporteAdapter(@NonNull Context context, List<Reporte> datos, OnReporteActionListener listener) {
        super(context, 0, datos);
        this.listener = listener;
        IP_SERVICIO = Config.getServerIp(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        // Inflar layout del item solo si no existe vista reciclada
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_reporte, parent, false);

        // Obtener reporte actual según posición
        Reporte r = getItem(position);

        // Referencias al layout
        TextView txtCat = convertView.findViewById(R.id.txtCategoria);
        TextView txtDesc = convertView.findViewById(R.id.txtDescripcion);
        TextView txtEstado = convertView.findViewById(R.id.txtEstado);
        ImageView img = convertView.findViewById(R.id.imgReporte);

        Button btnEditar = convertView.findViewById(R.id.btnEditar);
        Button btnBorrar = convertView.findViewById(R.id.btnBorrar);
        Button btnMarcarSolucionado = convertView.findViewById(R.id.btnMarcarSolucionado);

        // Colores de los botones
        btnEditar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F6CC83")));
        btnBorrar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F09B6C")));
        btnMarcarSolucionado.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8ABE87")));

        if (r != null) {
            // Asignar textos a vistas
            txtCat.setText(r.getCategoria());
            txtDesc.setText(r.getDescripcion());
            txtEstado.setText(r.getEstado());

            // Colores base para categoría y descripción
            txtCat.setTextColor(Color.parseColor("#000000"));
            txtDesc.setTextColor(Color.parseColor("#000000"));

            // Cambiar color del texto del estado según valor
            if (r.getEstado().equalsIgnoreCase("Pendiente")) {
                txtEstado.setTextColor(Color.parseColor("#FFA000")); // naranja
            } else if (r.getEstado().equalsIgnoreCase("Activo")) {
                txtEstado.setTextColor(Color.parseColor("#D32F2F")); // rojo
            } else if (r.getEstado().equalsIgnoreCase("Solucionado")) {
                txtEstado.setTextColor(Color.parseColor("#388E3C")); // verde
            } else {
                txtEstado.setTextColor(Color.parseColor("#888888")); // gris por defecto
            }

            // Cargar imagen usando Glide desde servidor
            String imagenUrlCompleta = "http://" + IP_SERVICIO + ":5000/" + r.getImagenUrl();
            Glide.with(getContext()).load(imagenUrlCompleta).into(img);

            // Mostrar botones según estado
            if (r.getEstado().equalsIgnoreCase("Activo")) {
                btnEditar.setVisibility(View.VISIBLE);
                btnBorrar.setVisibility(View.VISIBLE);
                btnMarcarSolucionado.setVisibility(View.GONE);
            } else if (r.getEstado().equalsIgnoreCase("Pendiente")) {
                btnEditar.setVisibility(View.GONE);
                btnBorrar.setVisibility(View.GONE);
                btnMarcarSolucionado.setVisibility(View.VISIBLE);
            } else {
                btnEditar.setVisibility(View.GONE);
                btnBorrar.setVisibility(View.GONE);
                btnMarcarSolucionado.setVisibility(View.GONE);
            }

            // Configurar eventos para los botones que llaman a la actividad mediante el listener
            btnEditar.setOnClickListener(v -> {
                if (listener != null) listener.onEditarReporte(r.getId());
            });

            btnBorrar.setOnClickListener(v -> {
                if (listener != null) listener.onBorrarReporte(r.getId());
            });

            btnMarcarSolucionado.setOnClickListener(v -> {
                if (listener != null) listener.onMarcarComoSolucionado(r.getId());
            });

            // Al tocar la imagen, abrir en pantalla completa
            img.setOnClickListener(v -> {
                String urlCompleta = "http://" + IP_SERVICIO + ":5000/" + r.getImagenUrl();
                Utils.abrirImagenPantallaCompleta(getContext(), urlCompleta);
            });

        }

        return convertView;
    }
}
