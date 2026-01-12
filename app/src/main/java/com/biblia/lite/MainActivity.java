package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private SQLiteDatabase dbBiblia;
    private UserDatabaseHelper userDbHelper;
    
    // Configuración de estado actual
    private String versionActual = "PDT.SQLite3"; 
    private int libroActual = 1;
    private int capituloActual = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inicializar base de datos de usuario (Crea user.db si no existe)
        userDbHelper = new UserDatabaseHelper(this);
        
        // 2. Configurar Interfaz
        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        
        setupNavigation();
        
        // 3. Cargar Biblia
        prepararYAbirBiblia();
    }

    private void prepararYAbirBiblia() {
        try {
            copiarArchivoDesdeAssets(versionActual);
            dbBiblia = SQLiteDatabase.openDatabase(getDatabasePath(versionActual).getPath(), null, SQLiteDatabase.OPEN_READONLY);
            cargarTexto();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar biblia: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copiarArchivoDesdeAssets(String nombre) throws Exception {
        File destino = getDatabasePath(nombre);
        if (!destino.exists()) {
            destino.getParentFile().mkdirs();
            InputStream is = getAssets().open(nombre);
            OutputStream os = new FileOutputStream(destino);
            byte[] buffer = new byte[1024];
            int largo;
            while ((largo = is.read(buffer)) > 0) {
                os.write(buffer, 0, largo);
            }
            os.flush(); os.close(); is.close();
        }
    }

    private void cargarTexto() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        try {
            // Consulta estándar para la mayoría de .SQLite3 de biblias
            Cursor c = dbBiblia.rawQuery("SELECT text FROM verses WHERE book_id=" + libroActual + " AND chapter=" + capituloActual, null);
            if (c.moveToFirst()) {
                do {
                    String clean = c.getString(0).replaceAll("<[^>]+>", "").trim();
                    listaVersiculos.add(clean);
                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception e) {
            listaVersiculos.add("Error en tablas: " + e.getMessage());
        }
        adapter = new VersiculoAdapter(listaVersiculos);
        rvVersiculos.setAdapter(adapter);
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) {
                abrirSelectorLibros();
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Ajustes próximamente", Toast.LENGTH_SHORT).show();
                return true;
            }
            return true;
        });
    }

    private void abrirSelectorLibros() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText("Selector de Libros (Próximamente)");
        bottomSheet.setContentView(view);
        bottomSheet.show();
    }
}
