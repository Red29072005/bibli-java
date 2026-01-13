package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvVersiculos;
    private VersiculoAdapter adapter;
    private List<String> listaVersiculos = new ArrayList<>();
    private SQLiteDatabase dbBiblia;
    private String versionActual = "NVI'22.SQLite3"; 
    private int libroId = 1, capituloActual = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        rvVersiculos.setBackgroundColor(Color.BLACK);
        setupNavigation();
        abrirBiblia();
    }

    // --- LOGICA DE CONSULTAS PERSONALIZADAS POR VERSION ---
    
    private String getTablaLibros() {
        // Aquí especificamos según la versión, tal como pediste
        if (versionActual.equals("PDT.SQLite3") || versionActual.equals("DHHS'94.SQLite3")) {
            return "books_all";
        } else {
            return "books";
        }
    }

    private void abrirBiblia() {
        try {
            if (dbBiblia != null) dbBiblia.close();
            File f = getDatabasePath(versionActual);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                InputStream is = getAssets().open(versionActual);
                FileOutputStream os = new FileOutputStream(f);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) > 0) os.write(buffer, 0, read);
                os.close(); is.close();
            }
            dbBiblia = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            render();
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir " + versionActual, Toast.LENGTH_SHORT).show();
        }
    }

    private void render() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        try {
            // 1. Obtener nombre del libro usando la tabla específica
            String tabla = getTablaLibros();
            Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM " + tabla + " WHERE book_number = " + libroId, null);
            if (cb.moveToFirst()) {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(cb.getString(0) + " " + capituloActual);
            }
            cb.close();

            // 2. Cargar Versículos
            Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number = " + libroId + " AND chapter = " + capituloActual + " ORDER BY verse ASC", null);
            
            while (c.moveToNext()) {
                String numV = c.getString(0);
                String txt = c.getString(1).replaceAll("<[^>]+>", "").replaceAll("\\[\\d+\\]", "").trim();
                listaVersiculos.add(numV + " " + txt);
            }
            c.close();

            adapter = new VersiculoAdapter(listaVersiculos, 18, true);
            rvVersiculos.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this, "Error de lectura en " + versionActual, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> nombres = new ArrayList<>();
        
        try {
            String tabla = getTablaLibros();
            Cursor c = dbBiblia.rawQuery("SELECT long_name FROM " + tabla + " ORDER BY book_number", null);
            while(c.moveToNext()) nombres.add(c.getString(0));
            c.close();
        } catch (Exception e) {
            nombres.add("Error al cargar lista");
        }

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            libroId = pos + 1;
            d.dismiss();
            mostrarSelectorCapitulos();
        });
        d.setContentView(lv);
        d.show();
    }

    // --- EL RESTO DE MÉTODOS SE MANTIENEN IGUAL ---

    private void mostrarSelectorCapitulos() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> caps = new ArrayList<>();
        try {
            Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number = " + libroId, null);
            int max = (c.moveToFirst()) ? c.getInt(0) : 1;
            c.close();
            for(int i=1; i<=max; i++) caps.add("Capítulo " + i);
        } catch (Exception e) {
            for(int i=1; i<=50; i++) caps.add("Capítulo " + i);
        }
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            capituloActual = pos + 1;
            d.dismiss();
            render();
        });
        d.setContentView(lv);
        d.show();
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) mostrarSelectorLibros();
            else if (id == R.id.nav_versions) mostrarSelectorVersiones();
            return true;
        });
    }

    private void mostrarSelectorVersiones() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] nombres = {"NVI", "LBLA", "DHHS", "PDT"};
        String[] archivos = {"NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3", "PDT.SQLite3"};
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            versionActual = archivos[pos];
            abrirBiblia();
            d.dismiss();
        });
        d.setContentView(lv);
        d.show();
    }
}