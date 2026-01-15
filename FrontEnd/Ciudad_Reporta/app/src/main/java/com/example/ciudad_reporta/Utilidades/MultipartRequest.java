package com.example.ciudad_reporta.Utilidades;

import com.android.volley.*;
import com.android.volley.toolbox.*;

import java.io.*;
import java.util.*;

public class MultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> listener;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final File archivoImagen;
    private final String campoArchivo;

    private final String boundary = "----CiudadReporta" + System.currentTimeMillis();

    // Constructor que inicializa los datos necesarios
    public MultipartRequest(String url,
                            Response.Listener<NetworkResponse> listener,
                            Response.ErrorListener errorListener,
                            Map<String, String> params,
                            File archivoImagen,
                            String campoArchivo) {
        super(Method.POST, url, errorListener);
        this.listener = listener;
        this.params = params;
        this.archivoImagen = archivoImagen;
        this.campoArchivo = campoArchivo;
        this.headers = new HashMap<>();
        headers.put("Content-Type", "multipart/form-data;boundary=" + boundary);
    }

    // Definir el tipo de contenido de la petición
    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }

    // Cuerpo de la petición HTTP (armado del multipart)
    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Campos normales
            for (Map.Entry<String, String> entry : params.entrySet()) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                dos.writeBytes(entry.getValue() + "\r\n");
            }

            // Archivo imagen
            if (archivoImagen != null) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"" + campoArchivo + "\"; filename=\"" + archivoImagen.getName() + "\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                FileInputStream fis = new FileInputStream(archivoImagen);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();
                dos.writeBytes("\r\n");
            }

            // Fin del cuerpo del mensaje
            dos.writeBytes("--" + boundary + "--\r\n");
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        listener.onResponse(response);
    }
}
