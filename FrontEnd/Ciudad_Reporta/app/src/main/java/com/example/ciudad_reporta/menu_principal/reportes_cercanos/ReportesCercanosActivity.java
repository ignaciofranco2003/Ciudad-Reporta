package com.example.ciudad_reporta.menu_principal.reportes_cercanos;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.Utilidades.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;


public class ReportesCercanosActivity extends AppCompatActivity {

    private String IP_SERVICIO;                 // IP del servidor backend
    private MapView map;                        // Mapa de la vista
    private final double RADIO_MAX_KM = 5.0;    //Radio máximo de búsqueda de reportes (en km)
    private Location ubicacionUsuario;          // Ubicación simulada del usuario
    private final Map<String, Integer> coordenadasUsadas = new HashMap<>();
    private static final int REQUEST_PERMISOS_UBICACION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtener IP desde clase Config
        IP_SERVICIO = Config.getServerIp(this);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_reportes_cercanos);

        map = findViewById(R.id.mapaReportes);
        map.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Verificar permiso de ubicación
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            obtenerUbicacionYMostrar();
        }

        // Botón de volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());


        ImageButton btnInfo = findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ReportesCercanosActivity.this)
                        .setTitle("Información")
                        .setMessage(R.string.info_mapa_reportes)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });
    }

    private void obtenerUbicacionYMostrar() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                ubicacionUsuario = location;

                map.getController().setZoom(15.0);
                map.getController().setCenter(new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude()));

            } else {
                // Si no se puede obtener, usar ubicación predeterminada
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.675);
                ubicacionUsuario.setLongitude(-59.66);
                map.getController().setZoom(15.0);
                map.getController().setCenter(new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude()));
            }

            cargarReportes();

        }).addOnFailureListener(e -> {
            // En caso de error, usar ubicación por defecto
            ubicacionUsuario = new Location("");
            ubicacionUsuario.setLatitude(-33.675);
            ubicacionUsuario.setLongitude(-59.66);
            map.getController().setZoom(15.0);
            map.getController().setCenter(new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude()));
            cargarReportes();
        });
    }

    // Cargar los reportes solucionados desde el servidor
    private void cargarReportes() {
        String url = "http://" + IP_SERVICIO + ":5000/reportes_solucionados";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Object json = new JSONTokener(response).nextValue();
                        if (json instanceof JSONArray) {
                            JSONArray reportes = (JSONArray) json;

                            if (reportes.length() == 0) {
                                Toast.makeText(this, "No hay reportes solucionados para mostrar", Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Iterar cada reporte y filtrar por distancia
                            for (int i = 0; i < reportes.length(); i++) {
                                JSONObject r = reportes.getJSONObject(i);
                                double lat = r.getDouble("latitud");
                                double lon = r.getDouble("longitud");
                                int idReporte = r.getInt("id_reporte");

                                Location locReporte = new Location("");
                                locReporte.setLatitude(lat);
                                locReporte.setLongitude(lon);

                                float distancia = ubicacionUsuario.distanceTo(locReporte) / 1000; // km

                                if (distancia <= RADIO_MAX_KM) {
                                    agregarMarker(idReporte, lat, lon); // Mostrar marcador
                                }
                            }
                        } else if (json instanceof JSONObject) {
                            JSONObject obj = (JSONObject) json;
                            String mensaje = obj.optString("mensaje", "No hay reportes solucionados para mostrar");
                            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar la respuesta", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 204) {
                        Toast.makeText(this, "No hay reportes solucionados para mostrar", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_LONG).show();
                    }
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void agregarMarker(int idReporte, double lat, double lon) {
        // Crear una clave única para lat/lon redondeada (evita errores por decimales)
        String clave = String.format("%.5f_%.5f", lat, lon);
        int repeticiones = coordenadasUsadas.getOrDefault(clave, 0);

        // Desplazar ligeramente en forma de espiral
        double offset = 0.00015; // aprox 15 metros
        double angle = repeticiones * 45; // en grados: 0°, 45°, 90°, 135°, etc.

        double latOffset = lat + (offset * Math.cos(Math.toRadians(angle)));
        double lonOffset = lon + (offset * Math.sin(Math.toRadians(angle)));

        // Guardar la nueva ocurrencia
        coordenadasUsadas.put(clave, repeticiones + 1);

        // Crear marcador con la nueva posición
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(latOffset, lonOffset));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Reporte #" + idReporte);

        Drawable iconoVerde = getResources().getDrawable(R.drawable.marker_verde, null);
        marker.setIcon(iconoVerde);

        marker.setOnMarkerClickListener((m, mv) -> {
            mostrarDetalles(idReporte);
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate();
    }

    // Mostrar los detalles de un reporte al tocar un marcador
    private void mostrarDetalles(int id) {
        String url = "http://"+IP_SERVICIO+":5000/detalles_reporte/" + id;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String categoria = response.getString("nombre_categoria");
                        String desc = response.getString("descripcion");
                        String imagen = response.getString("imagen_URL"); // ejemplo: "uploads/foto.jpg"

                        // Inflar layout del popup
                        View vistaDialogo = getLayoutInflater().inflate(R.layout.dialog_detalle_reporte, null);
                        TextView txtCat = vistaDialogo.findViewById(R.id.textCategoria);
                        TextView txtDesc = vistaDialogo.findViewById(R.id.textDescripcion);
                        ImageView img = vistaDialogo.findViewById(R.id.imagenReporte);

                        txtCat.setText("Categoría: " + categoria);
                        txtDesc.setText(desc);

                        String nombreImagen = imagen.substring(imagen.lastIndexOf("/") + 1); // ej: cloaca1.jpg
                        String urlImagenCompleta = "http://"+IP_SERVICIO+":5000/static/imagenes/" + nombreImagen;

                        // Cargar imagen con Glide
                        Glide.with(this).load(urlImagenCompleta).into(img);

                        // Hacer que la imagen se pueda abrir en pantalla completa
                        img.setOnClickListener(v -> {
                            Utils.abrirImagenPantallaCompleta(this, urlImagenCompleta);
                        });

                        // Aplicar diseño según modo claro/oscuro
                        int nightModeFlags = getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

                        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES)
                        {
                            vistaDialogo.setBackgroundResource(R.drawable.bg_popup_rounded_dark);
                            txtCat.setTextColor(Color.WHITE);
                            txtDesc.setTextColor(Color.LTGRAY);
                        } else {
                            vistaDialogo.setBackgroundResource(R.drawable.bg_rounded_white);
                            txtCat.setTextColor(Color.BLACK);
                            txtDesc.setTextColor(Color.DKGRAY);
                        }

                        // Mostrar popup con detalles
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Detalle del reporte #" + id)
                                .setView(vistaDialogo)
                                .setPositiveButton("Cerrar", null)
                                .show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar detalle", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error al obtener detalles del reporte", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    // Permiso otorgado o denegado
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYMostrar();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Usando ubicación por defecto.", Toast.LENGTH_LONG).show();
                // Usar ubicación por defecto
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.675);
                ubicacionUsuario.setLongitude(-59.66);
                map.getController().setZoom(15.0);
                map.getController().setCenter(new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude()));
                obtenerUbicacionYMostrar();
            }
        }
    }

}