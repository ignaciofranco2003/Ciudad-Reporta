package com.example.ciudad_reporta.menu_principal.mis_reportes;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.Utilidades.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class MapaMisReportes extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private String IP_SERVICIO;
    private int ID_USUARIO;
    private MapView map;
    private final double RADIO_MAX_KM = 5.0;
    private Location ubicacionUsuario;
    private final Map<String, Integer> coordenadasUsadas = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IP_SERVICIO = Config.getServerIp(this);
        ID_USUARIO = Config.getIdUsuario(this);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_mapa_mis_reportes);

        map = findViewById(R.id.mapaReportes);
        map.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar permiso de ubicación
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            obtenerUbicacionActual();
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                ubicacionUsuario = location;
            } else {
                // Si no se puede obtener, usar ubicación predeterminada
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.675);
                ubicacionUsuario.setLongitude(-59.66);
            }

            mostrarMapaYReportes();
        }).addOnFailureListener(e -> {
            // En caso de error, usar ubicación por defecto
            ubicacionUsuario = new Location("");
            ubicacionUsuario.setLatitude(-33.675);
            ubicacionUsuario.setLongitude(-59.66);
            mostrarMapaYReportes();
        });
    }

    private void mostrarMapaYReportes() {
        GeoPoint centro = new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude());
        IMapController controller = map.getController();
        controller.setZoom(15.0);
        controller.setCenter(centro);

        cargarReportes();
    }

    private void cargarReportes() {
        String url = "http://" + IP_SERVICIO + ":5000/reportes/" + ID_USUARIO;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject r = response.getJSONObject(i);
                            double lat = r.getDouble("latitud");
                            double lon = r.getDouble("longitud");
                            int idReporte = r.getInt("id_reporte");
                            String estado = r.getString("estado");

                            Location locReporte = new Location("");
                            locReporte.setLatitude(lat);
                            locReporte.setLongitude(lon);

                            float distancia = ubicacionUsuario.distanceTo(locReporte) / 1000;

                            if (distancia <= RADIO_MAX_KM) {
                                agregarMarker(idReporte, lat, lon, estado);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "Error al cargar reportes", Toast.LENGTH_LONG).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // Permiso otorgado o denegado
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Usando ubicación por defecto.", Toast.LENGTH_LONG).show();
                // Usar ubicación por defecto
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.675);
                ubicacionUsuario.setLongitude(-59.66);
                mostrarMapaYReportes();
            }
        }
    }

    private void agregarMarker(int idReporte, double lat, double lon, String estado) {
        // Crear una clave única para lat/lon redondeada (evita errores por decimales)
        String clave = String.format("%.5f_%.5f", lat, lon);
        int repeticiones = coordenadasUsadas.getOrDefault(clave, 0);

        // Desplazar ligeramente en forma de espiral
        double offset = 0.00015; // aprox 15 metros
        double angle = repeticiones * 45; // en grados: 0°, 45°, 90°, etc.

        double latOffset = lat + (offset * Math.cos(Math.toRadians(angle)));
        double lonOffset = lon + (offset * Math.sin(Math.toRadians(angle)));

        // Guardar la nueva ocurrencia
        coordenadasUsadas.put(clave, repeticiones + 1);

        // Crear marcador con la nueva posición
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(latOffset, lonOffset));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Reporte #" + idReporte);

        // Asignar ícono según estado
        Drawable icono;
        switch (estado.toLowerCase()) {
            case "pendiente":
                icono = getResources().getDrawable(R.drawable.marker_amarillo, null);
                break;
            case "solucionado":
                icono = getResources().getDrawable(R.drawable.marker_verde, null);
                break;
            default:
                icono = getResources().getDrawable(R.drawable.marker_rojo, null);
                break;
        }
        marker.setIcon(icono);

        // Al tocar el marcador, mostrar detalles
        marker.setOnMarkerClickListener((m, mv) -> {
            mostrarDetalles(idReporte);
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate();   // Redibujar mapa
    }

    // Mostrar detalles de un reporte específico al hacer clic en un marcador
    private void mostrarDetalles(int id) {
        String url = "http://"+IP_SERVICIO+":5000/detalles_reporte/" + id;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String categoria = response.getString("nombre_categoria");
                        String desc = response.getString("descripcion");
                        String imagen = response.getString("imagen_URL");
                        String estado = response.getString("estado");

                        // Inflar layout del diálogo
                        View vistaDialogo = getLayoutInflater().inflate(R.layout.dialog_detalle_reporte, null);
                        TextView txtCat = vistaDialogo.findViewById(R.id.textCategoria);
                        TextView txtDesc = vistaDialogo.findViewById(R.id.textDescripcion);
                        TextView txtEstado = vistaDialogo.findViewById(R.id.textEstado);
                        txtEstado.setVisibility(TextView.VISIBLE);
                        ImageView img = vistaDialogo.findViewById(R.id.imagenReporte);

                        txtCat.setText("Categoría: " + categoria);
                        txtDesc.setText(desc);
                        txtEstado.setText("Estado: "+ estado);

                        // Colores del texto del estado
                        if (estado.equalsIgnoreCase("activo")){
                            txtEstado.setTextColor(Color.RED);
                        } else if (estado.equalsIgnoreCase("pendiente")) {
                            txtEstado.setTextColor(Color.YELLOW);
                        }else{
                            txtEstado.setTextColor(Color.GREEN);
                        }

                        // Cargar imagen desde URL usando Glide
                        String nombreImagen = imagen.substring(imagen.lastIndexOf("/") + 1); // ej: cloaca1.jpg
                        String urlImagenCompleta = "http://"+IP_SERVICIO+":5000/static/imagenes/" + nombreImagen;

                        Glide.with(this).load(urlImagenCompleta).into(img);

                        // Hacer que la imagen se pueda abrir en pantalla completa
                        img.setOnClickListener(v -> {
                            Utils.abrirImagenPantallaCompleta(this, urlImagenCompleta);
                        });

                        // Cambiar estilo según tema claro/oscuro
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

                        // Mostrar el popup con la información
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

}