package com.example.ciudad_reporta.menu_principal.mis_reportes;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MisReportesActivity extends AppCompatActivity implements ReporteAdapter.OnReporteActionListener {

    // IP del servidor - ID del usuario
    private String IP_SERVICIO;
    private int ID_USUARIO;

    // Vistas y adaptador
    private ListView listaReportes;
    private List<Reporte> reportes = new ArrayList<>();
    private ReporteAdapter adapter;

    // Radio buttons para filtrar reportes
    private RadioGroup radioGrupo;
    private RadioButton rbActivos, rbPendientes, rbSolucionados;
    private MaterialButton btnVerMapa;
    private TextView txtMensaje;
    private static final int EDITAR_REPORTE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_reportes);

        txtMensaje = findViewById(R.id.txtMensaje);

        // Obtener IP e ID del usuario desde configuración local
        IP_SERVICIO = Config.getServerIp(this);
        ID_USUARIO = Config.getIdUsuario(this);

        // Botón para volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Inicialización de lista y adaptador
        listaReportes = findViewById(R.id.listaReportes);
        adapter = new ReporteAdapter(this, reportes, this);
        listaReportes.setAdapter(adapter);

        // Inicializar botones de filtro y sus estilos
        radioGrupo = findViewById(R.id.radioGrupoCategorias);
        rbActivos = findViewById(R.id.rbActivos);
        rbPendientes = findViewById(R.id.rbPendientes);
        rbSolucionados = findViewById(R.id.rbSolucionados);

        // Subrayar texto de los RadioButtons
        rbActivos.setPaintFlags(rbActivos.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        rbPendientes.setPaintFlags(rbPendientes.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        rbSolucionados.setPaintFlags(rbSolucionados.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Botón para ver reportes en el mapa
        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnVerMapa.setOnClickListener(v ->
            startActivity(new Intent(MisReportesActivity.this, MapaMisReportes.class))
        );

        // Por defecto: mostrar reportes activos
        rbActivos.setChecked(true);

        // Cambiar filtro según el botón seleccionado
        radioGrupo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbActivos) {
                aplicarFiltro("activo");
            } else if (checkedId == R.id.rbPendientes) {
                aplicarFiltro("pendiente");
            } else if (checkedId == R.id.rbSolucionados) {
                aplicarFiltro("solucionado");
            }
            listaReportes.clearChoices();   // Limpiar selección previa
            adapter.notifyDataSetChanged();
        });

        aplicarFiltro("activo");    // Filtro inicial

        // Acción al hacer clic sobre un item del listado
        listaReportes.setOnItemClickListener((parent, view, position, id) -> {
            Reporte reporte = reportes.get(position);
            if ("pendiente".equalsIgnoreCase(reporte.getEstado())) {
                mostrarDialogoFinalizar(reporte);
            } else if ("activo".equalsIgnoreCase(reporte.getEstado())) {
                Toast.makeText(this, "Editar (próximamente)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Obtener los reportes filtrados desde el backend
    private void aplicarFiltro(String filtro) {
        filtro = filtro.toLowerCase();
        String url = "http://" + IP_SERVICIO + ":5000/reportes/" + ID_USUARIO + "/" + filtro;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        reportes.clear();

                        if (response.has("mensaje")) {
                            txtMensaje.setText(response.getString("mensaje"));
                            txtMensaje.setVisibility(TextView.VISIBLE);
                            listaReportes.setVisibility(ListView.GONE);
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        // Cargar reportes en la lista
                        JSONArray array = response.getJSONArray("reportes");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Reporte r = new Reporte(
                                    obj.getInt("id_reporte"),
                                    obj.getString("nombre_categoria"),
                                    obj.getString("descripcion"),
                                    obj.getString("estado"),
                                    obj.getString("imagen_URL")
                            );
                            reportes.add(r);
                        }

                        txtMensaje.setVisibility(TextView.GONE);
                        listaReportes.setVisibility(ListView.VISIBLE);
                        adapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        txtMensaje.setText("Error al procesar los reportes");
                        txtMensaje.setVisibility(TextView.VISIBLE);
                        listaReportes.setVisibility(ListView.GONE);
                    }
                },
                error -> {
                    txtMensaje.setText("Error al cargar reportes filtrados");
                    txtMensaje.setVisibility(TextView.VISIBLE);
                    listaReportes.setVisibility(ListView.GONE);
                });

        queue.add(request);
    }

    // Obtener el filtro actual seleccionado
    private String getFiltroActual() {
        if (rbActivos.isChecked()) return "activo";
        if (rbPendientes.isChecked()) return "pendiente";
        if (rbSolucionados.isChecked()) return "solucionado";
        return "activo";
    }

    // Confirmar solución de reporte (cambiar su estado)
    private void mostrarDialogoFinalizar(Reporte reporte) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Finalizar reporte")
                .setMessage("¿El problema fue solucionado o persiste?")
                .setPositiveButton("Solucionado (Se marcara como solucionado)", (dialog, which) ->
                        enviarConfirmacionFinalizar(reporte.getId(), true))
                .setNegativeButton("Persiste (volvera a estar activo)", (dialog, which) ->
                        enviarConfirmacionFinalizar(reporte.getId(), false))
                .setNeutralButton("Cancelar", null)
                .show();
    }

    // Enviar al backend la confirmación para finalizar o reactivar un reporte
    private void enviarConfirmacionFinalizar(int idReporte, boolean solucionado) {
        String url = "http://" + IP_SERVICIO + ":5000/reportes/" + idReporte + "/finalizar";

        JSONObject datos = new JSONObject();
        try {
            datos.put("id_usuario", ID_USUARIO);
            datos.put("confirmacion", solucionado);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar datos", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, datos,
                response -> {
                    Toast.makeText(this, "Reporte actualizado", Toast.LENGTH_SHORT).show();
                    aplicarFiltro(getFiltroActual());
                },
                error -> Toast.makeText(this, "Error al finalizar reporte", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    // Acción de editar
    @Override
    public void onEditarReporte(int idReporte) {
        Intent intent = new Intent(this, EditarReporteActivity.class);
        intent.putExtra("id_reporte", idReporte);
        startActivityForResult(intent, EDITAR_REPORTE_REQUEST);
    }

    // Confirmar y borrar reporte
    @Override
    public void onBorrarReporte(int idReporte) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Seguro que desea borrar este reporte?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    borrarReporte(idReporte);
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Enviar solicitud DELETE al backend para borrar el reporte
    private void borrarReporte(int idReporte) {
        String url = "http://" + IP_SERVICIO + ":5000/borrar_reporte/" + idReporte +"/" + ID_USUARIO;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url,null,
                response -> {
                    Toast.makeText(this, "Reporte eliminado", Toast.LENGTH_SHORT).show();
                    aplicarFiltro(getFiltroActual());
                },
                error -> Toast.makeText(this, "Error al eliminar reporte", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // Marcar como solucionado desde el adaptador
    @Override
    public void onMarcarComoSolucionado(int idReporte) {
        mostrarDialogoFinalizar(new Reporte(idReporte, "", "", "pendiente", ""));
    }

    // Actualizar lista si se vuelve de la pantalla de edición
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDITAR_REPORTE_REQUEST && resultCode == RESULT_OK) {
            aplicarFiltro(getFiltroActual());
        }
    }
}