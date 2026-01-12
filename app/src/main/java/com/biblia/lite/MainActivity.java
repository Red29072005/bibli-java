package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvVersiculos;
    private VersiculoAdapter adapter;
    private List<String> listaVersiculos = new ArrayList<>();
    private int capituloActual = 1;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);

        copiarBaseDatos("RVR1960.db");
        abrirBaseDatos("RVR1960.db");

        cargarCapitulo();

        btnPrev.setOnClickListener(v -> {
            if (capituloActual > 1) {
                capituloActual--;
                cargarCapitulo();
            }
        });

        btnNext.setOnClickListener(v -> {
            capituloActual++;
            cargarCapitulo();
        });
    }

    private void copiarBaseDatos(String nombre) {
        File archivoDb = getDatabasePath(nombre);
        if (!archivoDb.exists()) {
            archivoDb.getParentFile().mkdirs();
            try {
                InputStream is = getAssets().open(nombre);
                OutputStream os = new FileOutputStream(archivoDb);
                byte[] buffer = new byte[1024];
                int largo;
                while ((largo = is.read(buffer)) > 0) {
                    os.write(buffer, 0, largo);
                }
                os.flush();
                os.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void abrirBaseDatos(String nombre) {
        db = SQLiteDatabase.openDatabase(getDatabasePath(nombre).getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private void cargarCapitulo() {
        listaVersiculos.clear();
        try {
            Cursor cursor = db.rawQuery("SELECT text FROM verses WHERE book_id=1 AND chapter=" + capituloActual, null);
            if (cursor.moveToFirst()) {
                do {
                    String rawText = cursor.getString(0);
                    String clean = rawText.replaceAll("<[^>]+>", "").trim();
                    listaVersiculos.add(clean);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            listaVersiculos.add("Error al cargar: " + e.getMessage());
        }
        
        adapter = new VersiculoAdapter(listaVersiculos);
        rvVersiculos.setAdapter(adapter);
    }
}
