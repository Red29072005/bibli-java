package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private SQLiteDatabase dbBiblia, dbUser;
    private Toolbar toolbar;
    
    private String versionActual = "PDT.SQLite3";
    private int libroId = 1, capituloActual = 1, fontSize = 18;
    private boolean isDarkMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initUserDB();
        cargarAjustes();
        
        // Aplicar modo noche antes de setContent
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        
        // Color de fondo manual para asegurar negro puro en modo oscuro
        rvVersiculos.setBackgroundColor(isDarkMode ? Color.BLACK : Color.WHITE);

        setupNavigation();
        abrirBiblia();
    }

    private void initUserDB() {
        dbUser = openOrCreateDatabase("user.db", MODE_PRIVATE, null);
        dbUser.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, val TEXT)");
        dbUser.execSQL("INSERT OR IGNORE INTO settings VALUES ('font_size', '18'), ('dark_mode', '1'), ('last_ver', 'PDT.SQLite3')");
    }

    private void cargarAjustes() {
        Cursor c = dbUser.rawQuery("SELECT key, val FROM settings", null);
        while(c.moveToNext()){
            String k = c.getString(0);
            if(k.equals("font_size")) fontSize = Integer.parseInt(c.getString(1));
            if(k.equals("dark_mode")) isDarkMode = c.getString(1).equals("1");
            if(k.equals("last_ver")) versionActual = c.getString(1);
        }
        c.close();
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
            Toast.makeText(this, "Error al cargar biblia", Toast.LENGTH_SHORT).show();
        }
    }

    private void render() {
        listaVersiculos.clear();
        // Obtener nombre del libro
        Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number = ?", new String[]{String.valueOf(libroId)});
        if (cb.moveToFirst()) {
            getSupportActionBar().setTitle(cb.getString(0) + " " + capituloActual);
        }
        cb.close();

        // Obtener versículos (según tu reporte, todas tienen book_number, chapter, verse, text)
        Cursor c = dbBiblia.rawQuery("SELECT text FROM verses WHERE book_number = ? AND chapter = ? ORDER BY verse ASC", 
            new String[]{String.valueOf(libroId), String.valueOf(capituloActual)});
        
        while (c.moveToNext()) {
            String txt = c.getString(0).replaceAll("<[^>]+>", "").trim();
            listaVersiculos.add(txt);
        }
        c.close();

        adapter = new VersiculoAdapter(listaVersiculos, fontSize, isDarkMode);
        rvVersiculos.setAdapter(adapter);
        
        if(listaVersiculos.isEmpty()) {
            Toast.makeText(this, "No hay texto en este capítulo", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) showBooksSelector();
            else if (id == R.id.nav_versions) showVersionsSelector();
            else if (id == R.id.nav_settings) showSettings();
            return true;
        });
    }

    private void showBooksSelector() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> libros = new ArrayList<>();
        
        Cursor c = dbBiblia.rawQuery("SELECT long_name FROM books ORDER BY book_number", null);
        while(c.moveToNext()) libros.add(c.getString(0));
        c.close();
        
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, libros));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            libroId = pos + 1; // Basado en el orden de book_number
            d.dismiss();
            showChaptersSelector();
        });
        d.setContentView(lv);
        d.show();
    }

    private void showChaptersSelector() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        
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

    private void showVersionsSelector() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] vers = {"PDT.SQLite3", "NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3"};
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vers));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            versionActual = vers[pos];
            dbUser.execSQL("UPDATE settings SET val=? WHERE key='last_ver'", new String[]{versionActual});
            abrirBiblia();
            d.dismiss();
        });
        d.setContentView(lv);
        d.show();
    }

    private void showSettings() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.layout_settings, null);
        
        SeekBar sb = v.findViewById(R.id.sbFont);
        Switch sw = v.findViewById(R.id.swTheme);
        
        sb.setProgress(fontSize - 12);
        sw.setChecked(isDarkMode);
        
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                fontSize = p + 12;
                dbUser.execSQL("UPDATE settings SET val=? WHERE key='font_size'", new String[]{String.valueOf(fontSize)});
                render();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        sw.setOnCheckedChangeListener((btn, checked) -> {
            isDarkMode = checked;
            dbUser.execSQL("UPDATE settings SET val=? WHERE key='dark_mode'", new String[]{checked ? "1" : "0"});
            recreate();
        });
        
        d.setContentView(v);
        d.show();
    }
}