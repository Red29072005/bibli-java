package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        rvVersiculos.setBackgroundColor(Color.BLACK);

        setupNavigation();
        abrirBiblia();
    }

    private void abrirBiblia() {
        try {
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
            Toast.makeText(this, "Error DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void render() {
        listaVersiculos.clear();
        // Obtener nombre largo del libro para el Toolbar
        Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number = ?", new String[]{String.valueOf(libroId)});
        if (cb.moveToFirst()) {
            getSupportActionBar().setTitle(cb.getString(0) + " " + capituloActual);
        }
        cb.close();

        // Consulta de versículos idéntica a obtener_texto en app.py 
        Cursor c = dbBiblia.rawQuery("SELECT text FROM verses WHERE book_number = ? AND chapter = ? ORDER BY verse ASC", 
            new String[]{String.valueOf(libroId), String.valueOf(capituloActual)});
        
        while (c.moveToNext()) {
            // Limpieza de etiquetas HTML/SQLite como en Python 
            String txt = c.getString(0).replaceAll("<[^>]+>", "").trim();
            listaVersiculos.add(txt);
        }
        c.close();

        adapter = new VersiculoAdapter(listaVersiculos, 18, true);
        rvVersiculos.setAdapter(adapter);
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

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> nombresLibros = new ArrayList<>();
        final List<Integer> idsLibros = new ArrayList<>();
        
        // Obtenemos book_number y nombre como en el orden_maestro de Python 
        Cursor c = dbBiblia.rawQuery("SELECT book_number, long_name FROM books ORDER BY book_number", null);
        while(c.moveToNext()) {
            idsLibros.add(c.getInt(0));
            nombresLibros.add(c.getString(1));
        }
        c.close();
        
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresLibros));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            libroId = idsLibros.get(pos);
            d.dismiss();
            mostrarSelectorCapitulos(); // Salta directamente a elegir capítulo
        });
        d.setContentView(lv);
        d.show();
    }

    private void mostrarSelectorCapitulos() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        
        // Lógica contar_capitulos de Python 
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number = ?", new String[]{String.valueOf(libroId)});
        int maxCap = c.moveToFirst() ? c.getInt(0) : 1;
        c.close();

        List<String> caps = new ArrayList<>();
        for(int i=1; i<=maxCap; i++) caps.add("Capítulo " + i);

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            capituloActual = pos + 1;
            render();
            d.dismiss();
        });
        d.setContentView(lv);
        d.show();
    }

    private void mostrarSelectorVersiones() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] nombres = {"NVI", "LBLA", "DHHS", "PDT"};
        final String[] archivos = {"NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3", "PDT.SQLite3"}; 
        
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
