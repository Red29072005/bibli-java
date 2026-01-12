package com.biblia.lite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    private SQLiteDatabase dbBiblia, dbUser, dbComm;
    private Toolbar toolbar;
    
    private String version = "PDT.SQLite3";
    private int libroId = 1, capitulo = 1, fontSize = 18;
    private boolean isDarkMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initUserDB();
        cargarAjustes();
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));

        setupNavigation();
        abrirBiblia();
    }

    private void initUserDB() {
        dbUser = openOrCreateDatabase("user.db", MODE_PRIVATE, null);
        dbUser.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, val TEXT)");
        dbUser.execSQL("CREATE TABLE IF NOT EXISTS favorites (book_id INTEGER, cap INTEGER, v_idx INTEGER)");
        dbUser.execSQL("INSERT OR IGNORE INTO settings VALUES ('font_size', '18'), ('dark_mode', '1'), ('last_ver', 'PDT.SQLite3')");
    }

    private void cargarAjustes() {
        Cursor c = dbUser.rawQuery("SELECT key, val FROM settings", null);
        while(c.moveToNext()){
            String k = c.getString(0);
            if(k.equals("font_size")) fontSize = Integer.parseInt(c.getString(1));
            if(k.equals("dark_mode")) isDarkMode = c.getString(1).equals("1");
            if(k.equals("last_ver")) version = c.getString(1);
        }
        c.close();
    }

    private void abrirBiblia() {
        try {
            copiarSiNoExiste(version);
            dbBiblia = SQLiteDatabase.openDatabase(getDatabasePath(version).getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // Abrir comentarios correspondientes según reporte 
            String commFile = version.replace(".SQLite3", ".commentaries.SQLite3");
            copiarSiNoExiste(commFile);
            dbComm = SQLiteDatabase.openDatabase(getDatabasePath(commFile).getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            render();
        } catch (Exception e) { Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }

    private void copiarSiNoExiste(String name) throws Exception {
        File f = getDatabasePath(name);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            InputStream is = getAssets().open(name);
            FileOutputStream os = new FileOutputStream(f);
            byte[] b = new byte[1024]; int l;
            while ((l = is.read(b)) > 0) os.write(b, 0, l);
            os.close(); is.close();
        }
    }

    private void render() {
        listaVersiculos.clear();
        String colLibro = existeColumna(dbBiblia, "verses", "book_number") ? "book_number" : "book_id";
        
        Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number=" + libroId, null);
        String nLibro = cb.moveToFirst() ? cb.getString(0) : "Libro";
        cb.close();
        getSupportActionBar().setTitle(nLibro + " " + capitulo);

        Cursor c = dbBiblia.rawQuery("SELECT text FROM verses WHERE " + colLibro + "=" + libroId + " AND chapter=" + capitulo, null);
        while (c.moveToNext()) listaVersiculos.add(c.getString(0).replaceAll("<[^>]+>", "").trim());
        c.close();

        adapter = new VersiculoAdapter(listaVersiculos, fontSize, isDarkMode, (pos) -> {
            marcar(pos);
            verComentario(pos + 1);
        });
        rvVersiculos.setAdapter(adapter);
    }

    private void marcar(int pos) {
        dbUser.execSQL("INSERT INTO favorites VALUES ("+libroId+","+capitulo+","+pos+")");
        Toast.makeText(this, "Subrayado guardado", Toast.LENGTH_SHORT).show();
    }

    private void verComentario(int vNum) {
        // Estructura según reporte: book_number, chapter_number_from, verse_number_from, text 
        Cursor c = dbComm.rawQuery("SELECT text FROM commentaries WHERE book_number=? AND chapter_number_from=? AND verse_number_from=?", 
                   new String[]{String.valueOf(libroId), String.valueOf(capitulo), String.valueOf(vNum)});
        
        if (c.moveToFirst()) {
            BottomSheetDialog d = new BottomSheetDialog(this);
            TextView tv = new TextView(this);
            tv.setPadding(40,40,40,40);
            tv.setText(c.getString(0).replaceAll("<[^>]+>", ""));
            d.setContentView(tv);
            d.show();
        }
        c.close();
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) showBooks();
            if (id == R.id.nav_versions) showVersions();
            if (id == R.id.nav_settings) showSettings();
            return true;
        });
    }

    private void showBooks() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> libros = new ArrayList<>();
        Cursor c = dbBiblia.rawQuery("SELECT long_name FROM books ORDER BY book_number", null);
        while(c.moveToNext()) libros.add(c.getString(0));
        c.close();
        
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, libros));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            libroId = pos + 1;
            d.dismiss();
            showChapters();
        });
        d.setContentView(lv);
        d.show();
    }

    private void showChapters() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        // Obtener cantidad de capítulos para el libro seleccionado
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
        int maxCap = c.moveToFirst() ? c.getInt(0) : 50;
        c.close();

        List<String> caps = new ArrayList<>();
        for(int i=1; i<=maxCap; i++) caps.add("Capítulo " + i);

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            capitulo = pos + 1;
            render();
            d.dismiss();
        });
        d.setContentView(lv);
        d.show();
    }

    private void showVersions() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] vers = {"PDT.SQLite3", "DHHS'94.SQLite3", "LBLA.SQLite3", "NVI'22.SQLite3"};
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vers));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            version = vers[pos];
            dbUser.execSQL("UPDATE settings SET val='"+version+"' WHERE key='last_ver'");
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
                dbUser.execSQL("UPDATE settings SET val='"+fontSize+"' WHERE key='font_size'");
                render();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        sw.setOnCheckedChangeListener((b, checked) -> {
            dbUser.execSQL("UPDATE settings SET val='"+(checked?"1":"0")+"' WHERE key='dark_mode'");
            recreate();
        });
        d.setContentView(v);
        d.show();
    }

    private boolean existeColumna(SQLiteDatabase db, String t, String col) {
        Cursor c = db.rawQuery("PRAGMA table_info("+t+")", null);
        while(c.moveToNext()) if(c.getString(1).equals(col)) return true;
        return false;
    }
}
