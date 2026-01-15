package com.example.ciudad_reporta.menu_principal.mis_reportes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditarReporteActivity extends AppCompatActivity {

    // Variables para campos del formulario y datos
    private int idReporte;
    private EditText etDescripcion;
    private Spinner spinnerCategoria;
    private Button btnGuardar;

    private String IP_SERVICIO;
    private int ID_USUARIO;
    private List<Categoria> categorias = new ArrayList<>();
    private ArrayAdapter<String> adapterCategorias;

    private TextView tvDescripcion, tvCategoria;

    // Clase para manejar categorías con id y nombre
    private static class Categoria {
        int id;
        String nombre;

        Categoria(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre; // Spinner muestra el nombre
        }
    }

    private Reporte reporte; // Guardar datos del reporte

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_reporte);

        // Obtener IP del backend e ID del usuario desde preferencias
        IP_SERVICIO = Config.getServerIp(this);
        ID_USUARIO = Config.getIdUsuario(this);

        // Botón de volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Obtener ID del reporte a editar
        idReporte = getIntent().getIntExtra("id_reporte", -1);

        // Referencias a vistas
        etDescripcion = findViewById(R.id.etDescripcion);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        btnGuardar = findViewById(R.id.btnGuardar);
        tvCategoria = findViewById(R.id.tvCategoria);
        tvDescripcion = findViewById(R.id.tvDescripcion);

        // Configurar adaptador vacío para el spinner
        adapterCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCategorias);

        // Estilo para el spinner
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(12); // Esquinas redondeadas en px
        gd.setStroke(4, Color.parseColor("#b1b7b8")); // ancho 4px, color borde
        spinnerCategoria.setBackground(gd);

        // Estilo para el botón
        btnGuardar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#81CE7B")));

        // Subrayar los títulos
        tvCategoria.setPaintFlags(tvCategoria.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDescripcion.setPaintFlags(tvDescripcion.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Cargar las categorías del backend
        cargarCategorias();

        // Botón guardar
        btnGuardar.setOnClickListener(v -> actualizarReporte());
    }

    // Cargar las categorías disponibles desde el backend
    private void cargarCategorias() {
        String url = "http://" + IP_SERVICIO + ":5000/categorias";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    categorias.clear();
                    List<String> nombresCategorias = new ArrayList<>();

                    // Recorrer cada categoría del JSON
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject catJson = response.getJSONObject(i);
                            int id = catJson.getInt("id");
                            String nombre = catJson.getString("nombre");
                            categorias.add(new Categoria(id, nombre));
                            nombresCategorias.add(nombre);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // Crear adaptador personalizado con las categorías
                    ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(
                            this, R.layout.spinner_item, categorias
                    );
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinnerCategoria.setAdapter(adapter);
                    adapterCategorias.clear();
                    adapterCategorias.addAll(nombresCategorias);
                    adapterCategorias.notifyDataSetChanged();

                    // Luego de cargar categorías, cargar el contenido del reporte
                    cargarDatosReporte();

                }, error -> {
            Toast.makeText(this, "Error cargando categorías", Toast.LENGTH_SHORT).show();
        });

        queue.add(request);
    }

    // Cargar datos actuales del reporte a editar
    private void cargarDatosReporte() {
        String url = "http://" + IP_SERVICIO + ":5000/detalles_reporte/" +idReporte;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Descripción
                        String descripcion = response.getString("descripcion");
                        etDescripcion.setText(descripcion);

                        // Categoría actual del reporte
                        String nombreCategoria = response.getString("nombre_categoria");

                        // Buscar la posición de la categoría por nombre
                        for (int i = 0; i < categorias.size(); i++) {
                            if (categorias.get(i).nombre.equalsIgnoreCase(nombreCategoria)) {
                                spinnerCategoria.setSelection(i);
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error al procesar datos del reporte", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error cargando reporte", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    // Enviar los datos actualizados del reporte al backend
    private void actualizarReporte() {
        String url = "http://" + IP_SERVICIO + ":5000/reportes/" + idReporte;

        String descripcion = etDescripcion.getText().toString();
        int posCategoria = spinnerCategoria.getSelectedItemPosition();

        if (posCategoria == -1) {
            Toast.makeText(this, "Seleccione una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        String categoriaSeleccionada = categorias.get(posCategoria).nombre;

        // Crear objeto JSON con los datos
        JSONObject datos = new JSONObject();
        try {
            datos.put("usuario_id", ID_USUARIO);
            datos.put("descripcion", descripcion);
            datos.put("nombre_categoria", categoriaSeleccionada);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al preparar datos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enviar PUT al servidor
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, datos,
                response -> {
                    Toast.makeText(this, "Reporte actualizado", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                },
                error -> Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }
}