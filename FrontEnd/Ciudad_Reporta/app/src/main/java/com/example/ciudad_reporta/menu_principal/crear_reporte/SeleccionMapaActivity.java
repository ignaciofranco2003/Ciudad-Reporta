package com.example.ciudad_reporta.menu_principal.crear_reporte;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.ciudad_reporta.R;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;


public class SeleccionMapaActivity extends AppCompatActivity {

    private MapView mapView;
    private GeoPoint puntoSeleccionado;
    private Marker marcador;
    private Button btnConfirmar;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private Location ubicacionUsuario;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar osmdroid con las preferencias del usuario
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_seleccion_mapa);

        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar permiso de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            obtenerUbicacionActual();
        }

        // Crear marcador (inicialmente invisible)
        marcador = new Marker(mapView);
        marcador.setDraggable(true);    // Permitido arrastrar

        // Botón de volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Ícono personalizado para el marcador
        Drawable iconoSeleccion = getResources().getDrawable(R.drawable.marker_azul, null);
        marcador.setIcon(iconoSeleccion);

        // Agregar marcador al mapa
        mapView.getOverlays().add(marcador);

        // Tocar en el mapa para seleccionar punto
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                IGeoPoint geoPoint = mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
                puntoSeleccionado = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());

                // Actualizar y mostrar el marcador en la posición tocada
                marcador.setPosition(puntoSeleccionado);
                marcador.setTitle("Ubicación seleccionada");
                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marcador.setVisible(true);
                mapView.invalidate();
            }
            return false;
        });

        // Confirmar selección de punto y devolver latitud/longitud
        btnConfirmar = findViewById(R.id.btnConfirmar);
        btnConfirmar.setOnClickListener(v -> {
            if (puntoSeleccionado != null) {
                Intent data = new Intent();
                data.putExtra("latitud", (float) puntoSeleccionado.getLatitude());
                data.putExtra("longitud", (float) puntoSeleccionado.getLongitude());
                setResult(RESULT_OK, data);
                finish();
            } else {
                Toast.makeText(this, "Seleccioná un punto en el mapa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void obtenerUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                ubicacionUsuario = location;
            } else {
                // Ubicación por defecto
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.6796);
                ubicacionUsuario.setLongitude(-59.6667);
            }

            centrarMapaEnUbicacion();
        }).addOnFailureListener(e -> {
            ubicacionUsuario = new Location("");
            ubicacionUsuario.setLatitude(-33.6796);
            ubicacionUsuario.setLongitude(-59.6667);
            centrarMapaEnUbicacion();
        });
    }

    private void centrarMapaEnUbicacion() {
        GeoPoint centro = new GeoPoint(ubicacionUsuario.getLatitude(), ubicacionUsuario.getLongitude());
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(centro);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                // Sin permisos → usar por defecto
                ubicacionUsuario = new Location("");
                ubicacionUsuario.setLatitude(-33.6796);
                ubicacionUsuario.setLongitude(-59.6667);
                centrarMapaEnUbicacion();
            }
        }
    }


}