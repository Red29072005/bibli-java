package com.biblia.lite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDatabaseHelper extends SQLiteOpenHelper {
    // Esto crea el archivo user.db automáticamente
    public UserDatabaseHelper(Context context) {
        super(context, "user.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tablas para tus marcadores y notas
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY AUTOINCREMENT, book_number INTEGER, chapter INTEGER, verse INTEGER, content TEXT)");
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, book_number INTEGER, chapter INTEGER, verse INTEGER, note TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
}
