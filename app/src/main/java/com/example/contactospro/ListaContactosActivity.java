package com.example.contactospro;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListaContactosActivity extends AppCompatActivity implements ContactoAdapter.OnItemClickListener {

    private RecyclerView recyclerViewContactos;
    private ContactoAdapter adapter;
    private DatabaseHelper db;
    private EditText editTextBuscar;
    private ImageButton btnAtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_contactos);

        db = new DatabaseHelper(this);

        recyclerViewContactos = findViewById(R.id.recyclerViewContactos);
        editTextBuscar = findViewById(R.id.editTextBuscar);
        btnAtras = findViewById(R.id.btnAtras);

        recyclerViewContactos.setLayoutManager(new LinearLayoutManager(this));
        cargarContactos();

        editTextBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAtras.setOnClickListener(v -> onBackPressed()); // Regresar a la actividad anterior
    }

    private void cargarContactos() {
        List<Contacto> contactos = db.getAllContactos();
        adapter = new ContactoAdapter(contactos, this);
        recyclerViewContactos.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar la lista cada vez que la actividad vuelve al primer plano
        cargarContactos();
    }

    @Override
    public void onItemClick(Contacto contacto) {
        mostrarDialogoAccionesContacto(contacto);
    }

    private void mostrarDialogoAccionesContacto(Contacto contacto) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_acciones_contacto, null);
        builder.setView(dialogView);

        TextView tvNombreContacto = dialogView.findViewById(R.id.tvNombreContactoDialog);
        Button btnLlamar = dialogView.findViewById(R.id.btnLlamar);
        Button btnCompartir = dialogView.findViewById(R.id.btnCompartir);
        Button btnVerImagen = dialogView.findViewById(R.id.btnVerImagen);
        Button btnEliminar = dialogView.findViewById(R.id.btnEliminar);
        Button btnActualizar = dialogView.findViewById(R.id.btnActualizar);

        tvNombreContacto.setText(getString(R.string.desea_llamar_a, contacto.getNombre()));

        AlertDialog dialog = builder.create();

        btnLlamar.setOnClickListener(v -> {
            dialog.dismiss();
            confirmarLlamada(contacto);
        });

        btnCompartir.setOnClickListener(v -> {
            dialog.dismiss();
            compartirContacto(contacto);
        });

        btnVerImagen.setOnClickListener(v -> {
            dialog.dismiss();
            verImagenContacto(contacto);
        });

        btnEliminar.setOnClickListener(v -> {
            dialog.dismiss();
            confirmarEliminarContacto(contacto);
        });

        btnActualizar.setOnClickListener(v -> {
            dialog.dismiss();
            // Implementa la lógica para actualizar, podrías abrir MainActivity con los datos del contacto
            abrirPantallaActualizar(contacto);
        });

        dialog.show();
    }

    private void confirmarLlamada(Contacto contacto) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.accion))
                .setMessage(getString(R.string.desea_llamar_a, contacto.getNombre()) + " " + contacto.getPais().split(" ")[1] + contacto.getTelefono() + "?")
                .setPositiveButton(getString(R.string.si), (dialog, which) -> {
                    iniciarLlamada(contacto.getPais().split(" ")[1] + contacto.getTelefono());
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void iniciarLlamada(String numeroTelefono) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + numeroTelefono.replace(" ", ""))); // Asegurarse de que no haya espacios
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no está concedido, solicitarlo
            Toast.makeText(this, "Permiso de llamada no concedido. Por favor, otórguelo en la configuración de la aplicación.", Toast.LENGTH_LONG).show();
            // Puedes agregar un requestPermissions aquí si lo deseas, pero el usuario ya debería haberlo concedido al inicio.
            return;
        }
        startActivity(callIntent);
    }

    private void compartirContacto(Contacto contacto) {
        String contactoInfo = "Nombre: " + contacto.getNombre() + "\n" +
                "País: " + contacto.getPais() + "\n" +
                "Teléfono: " + contacto.getTelefono() + "\n" +
                "Nota: " + contacto.getNota();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, contactoInfo);
        startActivity(Intent.createChooser(shareIntent, "Compartir contacto vía"));
    }

    private void verImagenContacto(Contacto contacto) {
        if (contacto.getImagenUri() != null && !contacto.getImagenUri().isEmpty()) {
            Uri imageUri = Uri.parse(contacto.getImagenUri());
            Intent viewImageIntent = new Intent(Intent.ACTION_VIEW);
            viewImageIntent.setDataAndType(imageUri, "image/*");
            viewImageIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Es crucial para acceder a URIs persistentes

            try {
                startActivity(viewImageIntent);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo abrir la imagen. Asegúrese de tener una aplicación para ver imágenes.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Este contacto no tiene una imagen asociada.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminarContacto(Contacto contacto) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Contacto")
                .setMessage("¿Estás seguro de que quieres eliminar a " + contacto.getNombre() + "?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    db.deleteContacto(contacto);
                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                    cargarContactos(); // Recargar la lista después de eliminar
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void abrirPantallaActualizar(Contacto contacto) {
        // Para actualizar, podrías reutilizar MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("contacto_id", contacto.getId());
        intent.putExtra("contacto_pais", contacto.getPais());
        intent.putExtra("contacto_nombre", contacto.getNombre());
        intent.putExtra("contacto_telefono", contacto.getTelefono());
        intent.putExtra("contacto_nota", contacto.getNota());
        intent.putExtra("contacto_imagen_uri", contacto.getImagenUri());
        startActivity(intent);
    }
}