package com.example.contactospro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LlamarActivity extends AppCompatActivity {

    private TextView textViewNumeroLlamando;
    private Button btnFinalizarLlamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_llamar);

        textViewNumeroLlamando = findViewById(R.id.textViewNumeroLlamando);
        btnFinalizarLlamada = findViewById(R.id.btnFinalizarLlamada);

        // Obtener el número de teléfono del Intent
        String numero = getIntent().getStringExtra("numero_telefono");
        if (numero != null) {
            textViewNumeroLlamando.setText(numero);
            // Aquí puedes iniciar la llamada real si no lo hiciste en la actividad anterior
            // Intent callIntent = new Intent(Intent.ACTION_CALL);
            // callIntent.setData(Uri.parse("tel:" + numero.replace(" ", "")));
            // if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            //     startActivity(callIntent);
            // } else {
            //     Toast.makeText(this, "Permiso de llamada no concedido.", Toast.LENGTH_SHORT).show();
            // }
        } else {
            Toast.makeText(this, "Número de teléfono no proporcionado.", Toast.LENGTH_SHORT).show();
            finish(); // Cerrar si no hay número
        }

        btnFinalizarLlamada.setOnClickListener(v -> {
            // Aquí podrías agregar lógica para "colgar" si fuera una llamada real.
            // Por ahora, simplemente cierra esta actividad.
            Toast.makeText(this, "Llamada finalizada (simulación)", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}