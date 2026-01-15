package com.example.ciudad_reporta.menu_principal.crear_reporte;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ciudad_reporta.R;
import com.example.ciudad_reporta.Utilidades.Config;
import com.example.ciudad_reporta.Utilidades.MultipartRequest;
import com.google.android.gms.location.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class CrearReporteActivity extends AppCompatActivity {

    // IP servidor - ID de usuario
    private String IP_SERVICIO;
    private int ID_USUARIO;
    private int indiceCategoriaRestaurada = -1;
    private int categoriaRestaurada = -1;
    private String descripcionRestaurada = "";
    private String imagenUriStrRestaurada = null;
    // Elementos del layout
    private Spinner spinnerCategoria, spinnerUbicacion;
    private EditText etDescripcion;
    private ImageView imgPreview;
    private Button btnSeleccionarImagen, btnSeleccionarUbicacion, btnEnviar;
    private TextView textViewUbicacion;
    private TextView tvTipoProblema;
    private TextView tvUbicacion;
    private TextView tvEvidencia;
    private TextView tvDescripcion;
    // Categorías cargadas desde el servidor
    private List<String> categoriasList = new ArrayList<>();
    private float latitud = 0, longitud = 0;
    // Manejo de imagen
    private Uri imagenUri = null;
    private File archivoImagen = null;
    // Constantes para solicitudes
    private static final int REQUEST_PERMISOS_UBICACION = 1001;
    private static final int REQUEST_UBICACION_MANUAL = 123;
    private static final int REQUEST_IMAGE_GALLERY = 1;
    private static final int REQUEST_IMAGE_CAMERA = 2;
    // Obtener la ubicación
    private FusedLocationProviderClient fusedLocationClient;
    // Diálogo de carga
    private ProgressDialog progressDialog;
    private boolean desdeSpinnerUbicacion = false;
    private static final int REQUEST_PERMISSION_CAMERA = 2001;
    private static final int REQUEST_PERMISSION_GALLERY = 2002;

    private int ultimaOpcionSeleccionada = -1;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.ciudad_reporta.R.layout.activity_crear_reporte);

        // Obtener IP y ID de usuario desde SharedPreferences
        IP_SERVICIO = Config.getServerIp(this);
        ID_USUARIO = Config.getIdUsuario(this);

        // Botón de volver
        ImageButton btnBack = findViewById(com.example.ciudad_reporta.R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Referencias a los elementos del layout
        textViewUbicacion = findViewById(com.example.ciudad_reporta.R.id.textViewUbicacion);
        //textViewLongitud = findViewById(R.id.textViewLongitud);
        spinnerCategoria = findViewById(com.example.ciudad_reporta.R.id.spinnerCategoria);
        spinnerUbicacion = findViewById(com.example.ciudad_reporta.R.id.spinnerUbicacion);
        etDescripcion = findViewById(com.example.ciudad_reporta.R.id.etDescripcion);
        imgPreview = findViewById(com.example.ciudad_reporta.R.id.imgPreview);
        btnSeleccionarImagen = findViewById(com.example.ciudad_reporta.R.id.btnSeleccionarImagen);
        btnSeleccionarUbicacion = findViewById(com.example.ciudad_reporta.R.id.btnSeleccionarUbicacion);
        btnSeleccionarUbicacion.setVisibility(View.GONE);
        tvTipoProblema = findViewById(com.example.ciudad_reporta.R.id.tvTipoProblema);
        tvUbicacion = findViewById(com.example.ciudad_reporta.R.id.tvUbicacion);
        tvDescripcion = findViewById(com.example.ciudad_reporta.R.id.tvDescripcion);
        tvEvidencia = findViewById(com.example.ciudad_reporta.R.id.tvEvidencia);
        btnEnviar = findViewById(com.example.ciudad_reporta.R.id.btnEnviar);

        // Cambiar color de fondo de botones
        btnEnviar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#81CE7B")));
        btnSeleccionarImagen.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F6CC83")));
        btnSeleccionarUbicacion.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F6CC83")));

        // Subrayar títulos de secciones
        tvTipoProblema.setPaintFlags(tvTipoProblema.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvUbicacion.setPaintFlags(tvUbicacion.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvDescripcion.setPaintFlags(tvDescripcion.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvEvidencia.setPaintFlags(tvEvidencia.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textViewUbicacion.setPaintFlags(textViewUbicacion.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Bordes redondeados para los spinners
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(12); // Esquinas redondeadas en px
        gd.setStroke(4, Color.parseColor("#b1b7b8")); // ancho 4px, color borde
        spinnerCategoria.setBackground(gd);
        spinnerUbicacion.setBackground(gd);

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Cargar categorías desde el servidor
        cargarCategorias();

        // Opciones para el tipo de ubicación
        String[] opciones = {"Seleccionar opción", "Manual", "Actual"};

        // Adaptador para spinner de ubicación
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, com.example.ciudad_reporta.R.layout.spinner_item, opciones) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.parseColor("#212121"));
                return view;
            }
        };

        adapter.setDropDownViewResource(com.example.ciudad_reporta.R.layout.spinner_dropdown_item);
        spinnerUbicacion.setAdapter(adapter);

        spinnerUbicacion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    btnSeleccionarUbicacion.setVisibility(View.GONE);
                    return;
                }
                if (position == 2) { // Ubicación automática
                    btnSeleccionarUbicacion.setVisibility(View.GONE);
                    desdeSpinnerUbicacion = true; // <- importante
                    verificarPermisoUbicacion();
                } else if (position == 1) { // Selección manual
                    btnSeleccionarUbicacion.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // Abrir mapa para seleccionar ubicación manualmente
        btnSeleccionarUbicacion.setOnClickListener(v -> {
            Intent intent = new Intent(this, SeleccionMapaActivity.class);
            startActivityForResult(intent, REQUEST_UBICACION_MANUAL);
        });

        // Mostrar opciones para cargar imagen
        btnSeleccionarImagen.setOnClickListener(v -> mostrarDialogoSeleccionImagen());

        // Enviar reporte al presionar el botón
        btnEnviar.setOnClickListener(v -> enviarReporte());
    }

    // Cargar categorías desde el servidor
    private void cargarCategorias() {
        String url = "http://" + IP_SERVICIO + ":5000/categorias";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> categorias = new ArrayList<>();
                    categorias.add("Seleccione una categoría");

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject categoria = response.getJSONObject(i);
                            categorias.add(categoria.getString("nombre"));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, com.example.ciudad_reporta.R.layout.spinner_item, categorias) {
                            @Override
                            public boolean isEnabled(int position) {
                                return position != 0; // Deshabilitar "Seleccione una categoría"
                            }

                            @Override
                            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                TextView tv = (TextView) view;
                                if (position == 0) {
                                    tv.setTextColor(Color.GRAY); // Mostrar como deshabilitado
                                } else {
                                    tv.setTextColor(Color.BLACK); // Normal
                                }
                                return view;
                            }
                        };

                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        spinnerCategoria.setAdapter(adapter);

                        // Cargar categorías en el spinner
                        /*ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, R.layout.spinner_item, categorias
                        );

                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        spinnerCategoria.setAdapter(adapter);*/

                        // Restaurar la selección si corresponde
                        if (categoriaRestaurada != -1 && categoriaRestaurada < categorias.size()) {
                            spinnerCategoria.setSelection(categoriaRestaurada);
                            categoriaRestaurada = -1; // limpiar luego de usar
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Error al procesar categorías", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void obtenerUbicacion() {
        // Verificar si los permisos de ubicación fueron otorgados
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Pedir permisos si no están dados
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISOS_UBICACION);
            return;
        }

        // Permisos otorgados: solicitar la última ubicación conocida
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitud = (float) location.getLatitude();
                longitud = (float) location.getLongitude();
                Log.d("UBICACION", "Ubicación: " + latitud + ", " + longitud);
                actualizarTextViewsUbicacion(latitud, longitud);
            } else {
                solicitarUbicacionNueva();
            }
        });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void solicitarUbicacionNueva() {
        // Configurar una solicitud de ubicación activa con alta precisión
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500);

        // Definir qué hacer cuando se obtiene una ubicación
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location location = result.getLastLocation();
                if (location != null) {
                    latitud = (float) location.getLatitude();
                    longitud = (float) location.getLongitude();
                    fusedLocationClient.removeLocationUpdates(this);
                    Log.d("UBICACION", "Ubicación activa: " + latitud + ", " + longitud);
                    actualizarTextViewsUbicacion(latitud, longitud);
                }
            }
        };
        // Solicitar actualizaciones de ubicación con el callback anterior
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void actualizarTextViewsUbicacion(float lat, float lon) {
        // Actualizar los TextViews que muestran latitud y longitud
        //textViewLatitud.setText("Latitud: " + lat);
        //textViewLongitud.setText("Longitud: " + lon);
        if (!(lat == 0 && lon == 0)) {
            convertirDireccion(lat,lon);
        }
        else{
            textViewUbicacion.setText("Direccion seleccionada: -");
        }

    }

/*
    private void mostrarDialogoSeleccionImagen() {
        // Opciones de selección de imagen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        String[] opciones = {"Galería", "Cámara"};
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                // Desde la galería
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
            } else {
                // Con cámara
                try {
                    archivoImagen = crearArchivoImagen();
                    if (archivoImagen != null) {
                        imagenUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", archivoImagen);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri);
                        startActivityForResult(intent, REQUEST_IMAGE_CAMERA);
                    }
                } catch (IOException ex) {
                    Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }
*/
    private void mostrarDialogoSeleccionImagen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        String[] opciones = {"Galería", "Cámara"};
        builder.setItems(opciones, (dialog, which) -> {
            ultimaOpcionSeleccionada = which;

            if (which == 0) {
                // Galería → verificar permiso
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_GALLERY);
                } else {
                    abrirGaleria();
                }

            } else if (which == 1) {
                // Cámara → verificar permiso
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_PERMISSION_CAMERA);
                } else {
                    abrirCamara();
                }
            }
        });
        builder.show();
    }
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
    }

    private void abrirCamara() {
        try {
            archivoImagen = crearArchivoImagen();
            if (archivoImagen != null) {
                imagenUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", archivoImagen);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAMERA);
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoImagen() throws IOException {
        // Crear un archivo temporal único para guardar la imagen
        String nombreArchivo = "imagen_" + System.currentTimeMillis();
        File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(nombreArchivo, ".jpg", directorio);
    }

    private File uriToFile(Uri uri) {
        // Convertir una Uri a un archivo físico en almacenamiento temporal
        try {
            File file = File.createTempFile("temp_image", ".jpg", getCacheDir());
            InputStream in = getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void enviarReporte() {
        // Obtener datos ingresados
        String categoria = spinnerCategoria.getSelectedItem() != null ? spinnerCategoria.getSelectedItem().toString() : "";
        String descripcion = etDescripcion.getText().toString().trim();

        // Validación de campos
        if (categoria.isEmpty() || descripcion.isEmpty() || latitud == 0 || longitud == 0 || archivoImagen == null) {
            Toast.makeText(this, "Complete todos los campos y espere la ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mensaje de progreso
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando reporte...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // URL para subir imagen
        String subirImagenUrl = "http://"+IP_SERVICIO+":5000/subir-imagen";

        // POST multipart para subir imagen
        MultipartRequest subirImagenRequest = new MultipartRequest(
                subirImagenUrl,
                response -> {
                    try {
                        String json = new String(response.data);
                        JSONObject jsonResponse = new JSONObject(json);
                        String imagenUrlFinal = jsonResponse.getString("url");

                        // Enviar el resto del reporte
                        enviarDatosReporte(imagenUrlFinal);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar URL de imagen", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show(),
                new HashMap<>(),
                archivoImagen,
                "imagen"  // nombre del campo que espera tu backend
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(subirImagenRequest);
    }

    private void enviarDatosReporte(String imagenUrl) {
        // Armar un JSON con todos los datos del reporte
        String url = "http://"+IP_SERVICIO+":5000/reporte";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("categoria", spinnerCategoria.getSelectedItem().toString());
            jsonBody.put("descripcion", etDescripcion.getText().toString().trim());
            jsonBody.put("latitud", latitud);
            jsonBody.put("longitud", longitud);
            jsonBody.put("imagen_URL", imagenUrl);
            jsonBody.put("usuario_id", ID_USUARIO);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al preparar JSON", Toast.LENGTH_SHORT).show();
            return;
        }

        // POST con el JSON al backend
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    progressDialog.dismiss(); // OCULTAR DIALOG
                    Toast.makeText(this, "Reporte enviado correctamente", Toast.LENGTH_LONG).show();
                    resetearFormulario(); // Limpiar campos
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error al enviar reporte", Toast.LENGTH_SHORT).show();
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void resetearFormulario() {
        // Limpiae todos los campos del formulario
        etDescripcion.setText("");
        imgPreview.setImageURI(null);
        imgPreview.setVisibility(View.GONE);
        imagenUri = null;
        archivoImagen = null;

        latitud = 0;
        longitud = 0;
        actualizarTextViewsUbicacion(0, 0);
        spinnerUbicacion.setSelection(0);

        if (spinnerCategoria.getAdapter() != null) {
            spinnerCategoria.setSelection(0);
        }

        // Borrar valores temporales guardados
        descripcionRestaurada = "";
        imagenUriStrRestaurada = null;
        indiceCategoriaRestaurada = -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                imagenUri = data.getData(); // Imagen desde galería
                archivoImagen = uriToFile(imagenUri);
            } else if (requestCode == REQUEST_IMAGE_CAMERA) {
                if (imagenUri != null) {
                    imgPreview.setImageURI(imagenUri);  // Muestra imagen capturada
                    imgPreview.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "No se pudo cargar la imagen de la cámara", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_UBICACION_MANUAL && data != null) {

                // Ubicación manual seleccionada
                latitud = data.getFloatExtra("latitud", 0f);
                longitud = data.getFloatExtra("longitud", 0f);

                if (latitud != 0f && longitud != 0f) {
                    //textViewLatitud.setText("Latitud: " + latitud);
                    convertirDireccion(latitud, longitud);
                    //textViewLongitud.setText("Longitud: " + longitud);
                } else {
                    textViewUbicacion.setText("Direccion seleccionada: -");
                    //textViewLongitud.setText("Longitud: -");

                }
            }

            // Mostrar la imagen si hay URI válida
            if (imagenUri != null) {
                imgPreview.setImageURI(imagenUri);
                imgPreview.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // U B I C A C I Ó N
        if (requestCode == REQUEST_PERMISOS_UBICACION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }

        // C Á M A R A
        else if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }

        // G A L E R Í A
        else if (requestCode == REQUEST_PERMISSION_GALLERY) {
                abrirGaleria();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Guardar el estado actual para recuperación
        outState.putFloat("latitud", latitud);
        outState.putFloat("longitud", longitud);
        outState.putString("descripcion", etDescripcion.getText().toString());

        if (imagenUri != null) {
            outState.putString("imagenUri", imagenUri.toString());
        }

        int posCategoria = spinnerCategoria.getSelectedItemPosition();
        outState.putInt("categoriaSeleccionada", posCategoria);

        System.out.println(posCategoria);
        System.out.println(latitud);
        System.out.println(longitud);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restaurar los datos guardados
        latitud = savedInstanceState.getFloat("latitud", 0f);
        longitud = savedInstanceState.getFloat("longitud", 0f);

        String uriStr = savedInstanceState.getString("imagenUri", null);
        if (uriStr != null) {
            imagenUri = Uri.parse(uriStr);
            imgPreview.setImageURI(imagenUri);
            imgPreview.setVisibility(View.VISIBLE);
            archivoImagen = uriToFile(imagenUri);
        }

        // Restaurar los campos de usuario
        etDescripcion.setText(savedInstanceState.getString("descripcion", ""));
        categoriaRestaurada = savedInstanceState.getInt("categoriaSeleccionada", -1);
        actualizarTextViewsUbicacion(latitud,longitud);
    }

    private void convertirDireccion(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, new Locale("es", "AR")); // Español de Argentina
        try {
            List<Address> direcciones = geocoder.getFromLocation(lat, lon, 1);

            if (direcciones != null && !direcciones.isEmpty()) {
                Address direccion = direcciones.get(0);

                String calle = direccion.getThoroughfare();       // Calle
                String numero = direccion.getSubThoroughfare();   // Número
                String ciudad = direccion.getLocality();          // Ciudad
                String provincia = direccion.getAdminArea();      // Provincia
                String pais = direccion.getCountryName();         // País

                // Armamos el formato: dirección n°, ciudad, provincia, país
                String direccionFormateada = "";

                if (calle != null) direccionFormateada += calle;
                if (numero != null) direccionFormateada += " " + numero;
                if (ciudad != null) direccionFormateada += ", " + ciudad;
                if (provincia != null) direccionFormateada += ", " + provincia;
                if (pais != null) direccionFormateada += ", " + pais;

                Log.d("DIRECCION", direccionFormateada);
                textViewUbicacion.setText("Direccion seleccionada: " + direccionFormateada);

            } else {
                Log.d("DIRECCION", "No se encontró ninguna dirección.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("DIRECCION", "Error al obtener la dirección: " + e.getMessage());
        }
    }

    private void verificarPermisoUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISOS_UBICACION);
        } else {
            obtenerUbicacion();
        }
    }

}