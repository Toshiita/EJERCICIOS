package com.example.contactospro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "contactosManager";
    private static final String TABLE_CONTACTOS = "contactos";

    // Nombres de las columnas de la tabla contactos
    private static final String KEY_ID = "id";
    private static final String KEY_PAIS = "pais";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_TELEFONO = "telefono";
    private static final String KEY_NOTA = "nota";
    private static final String KEY_IMAGEN_URI = "imagen_uri";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTOS_TABLE = "CREATE TABLE " + TABLE_CONTACTOS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_PAIS + " TEXT,"
                + KEY_NOMBRE + " TEXT,"
                + KEY_TELEFONO + " TEXT,"
                + KEY_NOTA + " TEXT,"
                + KEY_IMAGEN_URI + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTOS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTOS);
        onCreate(db);
    }

    // --- Operaciones CRUD (Crear, Leer, Actualizar, Borrar) ---

    // Añadir nuevo contacto
    public long addContacto(Contacto contacto) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PAIS, contacto.getPais());
        values.put(KEY_NOMBRE, contacto.getNombre());
        values.put(KEY_TELEFONO, contacto.getTelefono());
        values.put(KEY_NOTA, contacto.getNota());
        values.put(KEY_IMAGEN_URI, contacto.getImagenUri());

        long id = db.insert(TABLE_CONTACTOS, null, values);
        db.close();
        return id;
    }

    // Obtener un contacto por ID
    public Contacto getContacto(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONTACTOS, new String[]{KEY_ID,
                        KEY_PAIS, KEY_NOMBRE, KEY_TELEFONO, KEY_NOTA, KEY_IMAGEN_URI}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Contacto contacto = new Contacto(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5));
        cursor.close();
        return contacto;
    }

    // Obtener todos los contactos
    public List<Contacto> getAllContactos() {
        List<Contacto> contactoList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTOS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Contacto contacto = new Contacto();
                contacto.setId(Integer.parseInt(cursor.getString(0)));
                contacto.setPais(cursor.getString(1));
                contacto.setNombre(cursor.getString(2));
                contacto.setTelefono(cursor.getString(3));
                contacto.setNota(cursor.getString(4));
                contacto.setImagenUri(cursor.getString(5));
                contactoList.add(contacto);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return contactoList;
    }

    // Actualizar un contacto
    public int updateContacto(Contacto contacto) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PAIS, contacto.getPais());
        values.put(KEY_NOMBRE, contacto.getNombre());
        values.put(KEY_TELEFONO, contacto.getTelefono());
        values.put(KEY_NOTA, contacto.getNota());
        values.put(KEY_IMAGEN_URI, contacto.getImagenUri());

        int result = db.update(TABLE_CONTACTOS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(contacto.getId())});
        db.close();
        return result;
    }

    // Borrar un contacto
    public void deleteContacto(Contacto contacto) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTOS, KEY_ID + " = ?",
                new String[]{String.valueOf(contacto.getId())});
        db.close();
    }

    // Obtener el conteo de contactos
    public int getContactosCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CONTACTOS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}