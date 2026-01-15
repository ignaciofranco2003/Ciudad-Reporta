package com.example.ciudad_reporta.menu_principal.acerca_app;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.example.ciudad_reporta.R;

public class AcercaDeLaAppActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acerca_de_la_app);

        // Botón de volver
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Mostrar el contenido
        TextView textoAcerca = findViewById(R.id.textoAcerca);
        TextView textoFooter = findViewById(R.id.textoFooter);

        // Carga el texto principal con formato HTML desde strings.xml
        textoAcerca.setText(HtmlCompat.fromHtml(getString(R.string.texto_acerca_app), HtmlCompat.FROM_HTML_MODE_LEGACY));

        // Carga el texto del footer
        textoFooter.setText(getString(R.string.texto_footer));
    }
}
