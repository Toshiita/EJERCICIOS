package com.example.contactospro; // ¡¡CAMBIA ESTO!!

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Se eliminó REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSION ya que ahora se maneja con PERMISSION_REQUEST_CODE
    // y ActivityResultLauncher


    private EditText editTextNombre, editTextTelefono, editTextNota;
    private Spinner spinnerPais;
    private Button btnSalvarContacto, btnContactosSalvados;
    private ImageView imageViewPerfil;
    private ImageButton btnSeleccionarImagen;

    private DatabaseHelper db;
    private Uri selectedImageUri; // Ahora también almacena la URI de la foto tomada

    // Para manejar el resultado de la selección de imagen y tomar fotos (unificado)
    private ActivityResultLauncher<Intent> cameraAndGalleryLauncher;

    private int contactoIdParaActualizar = -1; // -1 si es nuevo, el ID si es para actualizar

    private static final int PERMISSION_REQUEST_CODE = 100; // Unificado para todos los permisos
    private String currentPhotoPath; // Para almacenar la ruta temporal de la foto tomada por la cámara

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Asegúrate de que el layout sea correcto

        db = new DatabaseHelper(this);

        // *** NOTA IMPORTANTE ***
        // Asegúrate de que tu res/values/strings.xml contenga estas cadenas:
        // <string name="alerta_nombre_vacio">El nombre no puede estar vacío</string>
        // <string name="alerta_telefono_vacio">El teléfono no puede estar vacío</string>
        // <string name="alerta_nota_vacia">La nota no puede estar vacía</string>
        // <string name="alerta_telefono_invalido">El teléfono debe tener 8 dígitos</string>
        // <string name="salvar_contacto">Salvar Contacto</string>
        // <string name="actualizar_contacto">Actualizar Contacto</string>
        // Y que res/array.xml (o res/values/arrays.xml) contenga:
        // <string-array name="paises_array">
        //     <item>Honduras</item>
        //     <item>El Salvador</item>
        //     <item>Guatemala</item>
        //     <item>Nicaragua</item>
        //     <item>Costa Rica</item>
        //     <item>Panamá</item>
        // </string-array>
        // Y que tengas un drawable ic_default_profile.xml para la imagen por defecto.


        editTextNombre = findViewById(R.id.editTextNombre);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextNota = findViewById(R.id.editTextNota);
        spinnerPais = findViewById(R.id.spinnerPais);
        btnSalvarContacto = findViewById(R.id.btnSalvarContacto);
        btnContactosSalvados = findViewById(R.id.btnContactosSalvados);
        imageViewPerfil = findViewById(R.id.imageViewPerfil);
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);

        // Configurar el Spinner con los países
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.paises_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPais.setAdapter(adapter);

        // Limitar el input del teléfono a 8 dígitos
        editTextTelefono.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});

        // Validaciones en tiempo real para cada campo
        setupEditTextValidation(editTextNombre, getString(R.string.alerta_nombre_vacio));
        setupEditTextValidation(editTextTelefono, getString(R.string.alerta_telefono_vacio));
        setupEditTextValidation(editTextNota, getString(R.string.alerta_nota_vacia));

        // Configurar ActivityResultLauncher para seleccionar imagen O tomar foto
        cameraAndGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null && result.getData().getData() != null) {
                            // Viene de la galería (ACTION_OPEN_DOCUMENT o ACTION_PICK)
                            selectedImageUri = result.getData().getData();
                            imageViewPerfil.setImageURI(selectedImageUri);
                            try {
                                // Persistir permisos para acceder a la URI de la galería
                                getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            } catch (SecurityException e) {
                                Toast.makeText(this, "No se pudo obtener permiso persistente para la imagen de la galería.", Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        } else if (currentPhotoPath != null) {
                            // Viene de la cámara (la imagen se guarda en currentPhotoPath)
                            selectedImageUri = Uri.fromFile(new File(currentPhotoPath));
                            // Cargar la imagen optimizada para el ImageView
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                                imageViewPerfil.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Error al cargar la imagen tomada.", Toast.LENGTH_SHORT).show();
                            }
                            // O simplemente
                            // imageViewPerfil.setImageURI(selectedImageUri);

                            // Para que la imagen sea visible en la galería del dispositivo (opcional)
                            galleryAddPic();
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
                try {
                    // Intentar cargar la URI persistente si es necesario
                    getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    imageViewPerfil.setImageURI(selectedImageUri);
                } catch (SecurityException | IllegalArgumentException e) {
                    // Si falla por permisos o URI inválida, cargar por defecto
                    imageViewPerfil.setImageResource(R.drawable.ic_default_profile);
                    selectedImageUri = null; // Limpiar URI inválida
                    Toast.makeText(this, "No se pudo cargar la imagen anterior. Seleccione una nueva.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                imageViewPerfil.setImageResource(R.drawable.ic_default_profile);
                selectedImageUri = null;
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
            // Solicitar permisos antes de mostrar el diálogo
            checkAndRequestPermissions(); // Llama a la función que pide permisos
            // El diálogo se mostrará solo si los permisos ya están concedidos o después de que el usuario los conceda
            // La lógica para mostrar el diálogo se moverá a onRequestPermissionsResult o se asumirá que se llama después de los permisos
            showImageSourceDialog();
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
        contactoIdParaActualizar = -1; // Resetear para futuros nuevos contactos
        btnSalvarContacto.setText(getString(R.string.salvar_contacto)); // Restablecer texto del botón
    }

    // --- Métodos de Imagen ---

    // Diálogo para que el usuario elija la fuente de la imagen
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Fuente de Imagen");
        builder.setItems(new CharSequence[]{"Tomar Foto", "Seleccionar de Galería"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Tomar Foto
                        dispatchTakePictureIntent();
                        break;
                    case 1: // Seleccionar de Galería
                        openGallery();
                        break;
                }
            }
        });
        builder.show();
    }

    // Iniciar la cámara para tomar una foto
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Asegúrate de que haya una actividad de cámara para manejar el intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Crea el archivo donde la foto debería ir
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error al crear el archivo
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
                ex.printStackTrace();
            }
            // Continúa solo si el archivo fue creado exitosamente
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.yourpackage.fileprovider", // *** CAMBIA ESTO AL NOMBRE DE TU PAQUETE + .fileprovider ***
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraAndGalleryLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    // Crear un archivo de imagen temporal para guardar la foto tomada por la cámara
    private File createImageFile() throws IOException {
        // Crea un nombre de archivo de imagen único
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Guarda la ruta de archivo para usarla cuando la cámara retorne el resultado
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Añadir la foto a la galería del dispositivo (opcional, para que el usuario la vea después)
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    // Abrir la galería para seleccionar una imagen
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // Usar ACTION_OPEN_DOCUMENT para mejor compatibilidad con Scoped Storage
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        // FLAG_GRANT_PERSISTABLE_URI_PERMISSION es importante para que la URI sea válida
        // después de que la app se reinicie y se pueda mantener el acceso.
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cameraAndGalleryLauncher.launch(intent);
    }

    // --- Manejo de Permisos ---

    private void checkAndRequestPermissions() {
        // Lista de permisos que necesitamos
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE // Para versiones anteriores a Android 10
                // Si tu targetSdkVersion es 33 o superior, considera agregar READ_MEDIA_IMAGES para Android 13+
                // Manifest.permission.READ_MEDIA_IMAGES
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
        // Si ya están concedidos, no se hace nada aquí. El diálogo se muestra por el onClickListener
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permisos concedidos!", Toast.LENGTH_SHORT).show();
                // Si los permisos se concedieron en este momento, podrías mostrar el diálogo aquí
                // si el usuario lo esperaría inmediatamente después de conceder los permisos.
                // showImageSourceDialog(); // Descomentar si quieres que se abra el diálogo justo después de conceder
            } else {
                Toast.makeText(this, "Algunos permisos fueron denegados. No se podrá usar la cámara o galería.", Toast.LENGTH_LONG).show();
            }
        }
    }
}