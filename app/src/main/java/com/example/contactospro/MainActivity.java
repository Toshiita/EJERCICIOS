package com.example.contactospro; // ¡¡CAMBIA ESTO!!

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION = 100;

    private EditText editTextNombre, editTextTelefono, editTextNota;
    private Spinner spinnerPais;
    private Button btnSalvarContacto, btnContactosSalvados;
    private ImageView imageViewPerfil;
    private ImageButton btnSeleccionarImagen;

    private DatabaseHelper db;
    private Uri selectedImageUri;

    // Para manejar el resultado de la selección de imagen
    private ActivityResultLauncher<Intent> pickImageLauncher;

    private int contactoIdParaActualizar = -1; // -1 si es nuevo, el ID si es para actualizar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Asegúrate de que el layout sea correcto

        db = new DatabaseHelper(this);

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextNota = findViewById(R.id.editTextNota);
        spinnerPais = findViewById(R.id.spinnerPais);
        btnSalvarContacto = findViewById(R.id.btnSalvarContacto);
        btnContactosSalvados = findViewById(R.id.btnContactosSalvados);
        imageViewPerfil = findViewById(R.id.imageViewPerfil);
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);

        // Limitar el input del teléfono a 8 dígitos
        editTextTelefono.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

        // Validaciones en tiempo real para cada campo (ejemplos)
        setupEditTextValidation(editTextNombre, getString(R.string.alerta_nombre_vacio));
        setupEditTextValidation(editTextTelefono, getString(R.string.alerta_telefono_vacio));
        setupEditTextValidation(editTextNota, getString(R.string.alerta_nota_vacia));

        // Configurar ActivityResultLauncher para seleccionar imagen
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imageViewPerfil.setImageURI(selectedImageUri);
                        // Asegurarse de que la URI sea persistente
                        // Esto es crucial para poder acceder a la imagen después de que la app se cierra.
                        // Sin embargo, puede haber limitaciones en versiones recientes de Android (Scoped Storage).
                        // Considera alternativas como guardar la imagen en el almacenamiento interno de la app
                        // o usar MediaStore para imágenes compartidas de forma más robusta.
                        try {
                            getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (SecurityException e) {
                            Toast.makeText(this, "No se pudo obtener permiso persistente para la imagen.", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                }
        );

        // --- Lógica para Cargar Contacto para Edición ---
        if (getIntent().hasExtra("contacto_id")) {
            contactoIdParaActualizar = getIntent().getIntExtra("contacto_id", -1);
            String pais = getIntent().getStringExtra("contacto_pais");
            String nombre = getIntent().getStringExtra("contacto_nombre");
            String telefono = getIntent().getStringExtra("contacto_telefono");
            String nota = getIntent().getStringExtra("contacto_nota");
            String imagenUriString = getIntent().getStringExtra("contacto_imagen_uri");

            editTextNombre.setText(nombre);
            editTextTelefono.setText(telefono);
            editTextNota.setText(nota);

            // Establecer el Spinner
            String[] paisesArray = getResources().getStringArray(R.array.paises_array);
            for (int i = 0; i < paisesArray.length; i++) {
                if (paisesArray[i].equals(pais)) {
                    spinnerPais.setSelection(i);
                    break;
                }
            }

            if (imagenUriString != null && !imagenUriString.isEmpty()) {
                selectedImageUri = Uri.parse(imagenUriString);
                imageViewPerfil.setImageURI(selectedImageUri);
            } else {
                imageViewPerfil.setImageResource(R.drawable.ic_default_profile);
            }
            btnSalvarContacto.setText(getString(R.string.actualizar_contacto)); // Cambiar texto del botón
        } else {
            btnSalvarContacto.setText(getString(R.string.salvar_contacto)); // Texto por defecto
        }
        // --- Fin Lógica Edición ---


        btnSalvarContacto.setOnClickListener(v -> {
            if (contactoIdParaActualizar != -1) {
                actualizarContacto();
            } else {
                salvarContacto();
            }
        });

        btnContactosSalvados.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListaContactosActivity.class); // Iniciar la actividad de la lista
            startActivity(intent);
        });

        btnSeleccionarImagen.setOnClickListener(v -> {
            // Solicitar permiso de almacenamiento si no está concedido
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION);
            } else {
                abrirGaleria();
            }
        });
    }

    private void setupEditTextValidation(EditText editText, String errorMessage) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    editText.setError(errorMessage);
                } else {
                    editText.setError(null);
                }
                // Validación específica para el teléfono
                if (editText.getId() == R.id.editTextTelefono) {
                    if (s.length() > 0 && s.length() < 8) { // Solo mostrar error si hay algo escrito y es menos de 8
                        editText.setError(getString(R.string.alerta_telefono_invalido));
                    } else {
                        editText.setError(null);
                    }
                }
            }
        });
    }

    private void salvarContacto() {
        String pais = spinnerPais.getSelectedItem().toString();
        String nombre = editTextNombre.getText().toString().trim();
        String telefono = editTextTelefono.getText().toString().trim();
        String nota = editTextNota.getText().toString().trim();
        String imagenUri = selectedImageUri != null ? selectedImageUri.toString() : null;

        if (nombre.isEmpty()) {
            editTextNombre.setError(getString(R.string.alerta_nombre_vacio));
            return;
        }
        if (telefono.isEmpty()) {
            editTextTelefono.setError(getString(R.string.alerta_telefono_vacio));
            return;
        }
        if (telefono.length() < 8) {
            editTextTelefono.setError(getString(R.string.alerta_telefono_invalido));
            return;
        }
        if (nota.isEmpty()) {
            editTextNota.setError(getString(R.string.alerta_nota_vacia));
            return;
        }

        Contacto nuevoContacto = new Contacto(0, pais, nombre, telefono, nota, imagenUri);
        long id = db.addContacto(nuevoContacto);

        if (id != -1) {
            Toast.makeText(this, "Contacto guardado exitosamente!", Toast.LENGTH_SHORT).show();
            limpiarCampos();
        } else {
            Toast.makeText(this, "Error al guardar el contacto", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarContacto() {
        String pais = spinnerPais.getSelectedItem().toString();
        String nombre = editTextNombre.getText().toString().trim();
        String telefono = editTextTelefono.getText().toString().trim();
        String nota = editTextNota.getText().toString().trim();
        String imagenUri = selectedImageUri != null ? selectedImageUri.toString() : null;

        if (nombre.isEmpty()) {
            editTextNombre.setError(getString(R.string.alerta_nombre_vacio));
            return;
        }
        if (telefono.isEmpty()) {
            editTextTelefono.setError(getString(R.string.alerta_telefono_vacio));
            return;
        }
        if (telefono.length() < 8) {
            editTextTelefono.setError(getString(R.string.alerta_telefono_invalido));
            return;
        }
        if (nota.isEmpty()) {
            editTextNota.setError(getString(R.string.alerta_nota_vacia));
            return;
        }

        Contacto contactoActualizado = new Contacto(contactoIdParaActualizar, pais, nombre, telefono, nota, imagenUri);
        int filasAfectadas = db.updateContacto(contactoActualizado);

        if (filasAfectadas > 0) {
            Toast.makeText(this, "Contacto actualizado exitosamente!", Toast.LENGTH_SHORT).show();
            finish(); // Cierra esta actividad y regresa a la lista
        } else {
            Toast.makeText(this, "Error al actualizar el contacto", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarCampos() {
        editTextNombre.setText("");
        editTextTelefono.setText("");
        editTextNota.setText("");
        spinnerPais.setSelection(0); // Seleccionar el primer elemento
        imageViewPerfil.setImageResource(R.drawable.ic_default_profile); // Restablecer imagen por defecto
        selectedImageUri = null;
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // FLAG_GRANT_PERSISTABLE_URI_PERMISSION es importante para que la URI sea válida
        // después de que la app se reinicie.
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirGaleria();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado. No se puede seleccionar imagen.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}