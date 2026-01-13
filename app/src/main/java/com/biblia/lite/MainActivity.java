package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
            Toast.makeText(this, "Error abriendo base de datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void render() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        try {
            // Intentamos obtener el nombre del libro de forma segura
            String nombreLibro = "Libro " + libroId;
            Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number = LIMIT 1", null);
            // Si falla la tabla 'books', intentamos 'books_all' (como en la PDT)
            try {
                cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number = ?", new String[]{String.valueOf(libroId)});
                if (cb.moveToFirst()) nombreLibro = cb.getString(0);
            } catch (Exception e) {
                cb = dbBiblia.rawQuery("SELECT long_name FROM books_all WHERE book_number = ?", new String[]{String.valueOf(libroId)});
                if (cb.moveToFirst()) nombreLibro = cb.getString(0);
            }
            cb.close();
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(nombreLibro + " " + capituloActual);

            // CONSULTA DE TEXTO (Blindada)
            Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number = ? AND chapter = ? ORDER BY verse ASC", 
                new String[]{String.valueOf(libroId), String.valueOf(capituloActual)});
            
            int colText = c.getColumnIndex("text");
            int colVerse = c.getColumnIndex("verse");

            while (c.moveToNext()) {
                String numV = (colVerse != -1) ? c.getString(colVerse) : String.valueOf(c.getPosition() + 1);
                String txt = (colText != -1) ? c.getString(colText) : "Error en columna text";
                
                // Limpieza idéntica a tu app.py
                txt = txt.replaceAll("<[^>]+>", "").replaceAll("\\[\\d+\\]", "").trim();
                listaVersiculos.add(numV + " " + txt);
            }
            c.close();

            adapter = new VersiculoAdapter(listaVersiculos, 18, true);
            rvVersiculos.setAdapter(adapter);

        } catch (Exception e) {
            Log.e("ERRO_BIBLIA", e.getMessage());
            Toast.makeText(this, "Error al cargar capítulos", Toast.LENGTH_LONG).show();
        }
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
        List<String> nombres = new ArrayList<>();
        // Lógica para obtener libros de cualquier tabla disponible
        try {
            Cursor c = dbBiblia.rawQuery("SELECT long_name FROM books ORDER BY book_number", null);
            while(c.moveToNext()) nombres.add(c.getString(0));
            c.close();
        } catch (Exception e) {
            Cursor c = dbBiblia.rawQuery("SELECT long_name FROM books_all ORDER BY book_number", null);
            while(c.moveToNext()) nombres.add(c.getString(0));
            c.close();
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

    private void mostrarSelectorCapitulos() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number = ?", new String[]{String.valueOf(libroId)});
        int maxCap = (c.moveToFirst()) ? c.getInt(0) : 50;
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