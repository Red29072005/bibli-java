package com.biblia.lite;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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
    private SQLiteDatabase dbBiblia, dbNotas;
    private String versionActual = "NVI'22.SQLite3"; 
    private int libroId = 1, capituloActual = 1, tamanoLetra = 18;
    private String tipoFuente = "SANS";
    private boolean modoOscuro = true;
    private TextView txtNavAbrev;
    private SharedPreferences prefs;

    private final String[] ORDEN_LIBROS = {
        "Génesis", "Éxodo", "Levítico", "Números", "Deuteronomio", "Josué", "Jueces", "Job",
        "Rut", "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "Salmo", "Proverbios", 
        "Eclesiastés", "Cantares", "1 Crónicas", "2 Crónicas", "Joel", "Amós", "Oseas", 
        "Miqueas", "Nahúm", "Jonás", "Habacuc", "Isaías", "Sofonías", "Jeremías", 
        "Lamentaciones", "Abdías", "Daniel", "Ezequiel", "Ester", "Hageo", "Zacarías", 
        "Malaquías", "Esdras", "Nehemías", "Mateo", "Marcos", "Lucas", "Juan", "Hechos", 
        "Romanos", "1 Corintios", "2 Corintios", "Gálatas", "Efesios", "Filipenses", 
        "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", 
        "Tito", "Filemón", "Hebreos", "Santiago", "1 Pedro", "2 Pedro", "1 Juan", 
        "2 Juan", "3 Juan", "Judas", "Apocalipsis"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("BibliaPrefs", MODE_PRIVATE);
        versionActual = prefs.getString("last_version", "NVI'22.SQLite3");
        libroId = prefs.getInt("last_book", 1);
        capituloActual = prefs.getInt("last_cap", 1);
        tamanoLetra = prefs.getInt("font_size", 18);
        modoOscuro = prefs.getBoolean("dark_mode", true);

        rvVersiculos = findViewById(R.id.rvVersiculos);
        txtNavAbrev = findViewById(R.id.txtNavAbrev);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAnterior).setOnClickListener(v -> cambiarCapitulo(-1));
        findViewById(R.id.btnSiguiente).setOnClickListener(v -> cambiarCapitulo(1));
        findViewById(R.id.btnAjustes).setOnClickListener(v -> mostrarAjustes());

        abrirBiblia();
        setupNavigation();
        aplicarTema();
    }

    private void render() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        try {
            txtNavAbrev.setText(obtenerAbrev(libroId) + " " + capituloActual);

            Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number=" + libroId + " AND chapter=" + capituloActual + " ORDER BY verse ASC", null);
            while (c.moveToNext()) {
                int nV = c.getInt(0);
                String raw = c.getString(1);
                
                // DETECCIÓN DE SALTOS (Párrafos, Títulos de Salmos, Diálogos)
                String salto = "";
                if (raw.contains("<pb") || raw.contains("<p") || raw.contains("<d") || raw.contains("<t") || raw.contains("<s")) {
                    salto = "\n\n"; // Doble salto para títulos y párrafos nuevos
                } else if (raw.contains("<q") || raw.contains("—")) {
                    salto = "\n";   // Salto simple para poesía y diálogos
                }

                String txt = raw.replaceAll("<[^>]+>", "").replaceAll("\\[\\d+\\]", "").trim();
                
                // Consultar color guardado en DB de notas
                String col = obtenerColor(nV);
                listaVersiculos.add(salto + nV + " " + txt + (col.isEmpty() ? "" : " ##"+col));
            }
            c.close();

            adapter = new VersiculoAdapter(listaVersiculos, tamanoLetra, modoOscuro, tipoFuente);
            adapter.setOnItemClickListener(pos -> {
                String line = listaVersiculos.get(pos).trim();
                int vNum = Integer.parseInt(line.split(" ")[0]);
                mostrarSelectorColores(vNum);
            });
            rvVersiculos.setAdapter(adapter);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String obtenerAbrev(int id) {
        Cursor c = dbBiblia.rawQuery("SELECT short_name FROM books WHERE book_number=" + id, null);
        String res = "LIB";
        if (c.moveToFirst()) res = c.getString(0).toUpperCase();
        c.close();
        return res;
    }

    private void cambiarCapitulo(int delta) {
        int nCap = capituloActual + delta;
        if (nCap < 1) return;
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
        if (c.moveToFirst() && nCap <= c.getInt(0)) {
            capituloActual = nCap;
            prefs.edit().putInt("last_cap", capituloActual).apply();
            render();
            rvVersiculos.scrollToPosition(0);
        }
        c.close();
    }

    private void abrirBiblia() {
        try {
            File f = new File(getFilesDir(), versionActual);
            if (!f.exists()) {
                InputStream is = getAssets().open(versionActual);
                FileOutputStream os = new FileOutputStream(f);
                byte[] b = new byte[1024]; int l;
                while((l=is.read(b))>0) os.write(b,0,l);
                os.close(); is.close();
            }
            dbBiblia = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            File fN = new File(getFilesDir(), "user_notes.db");
            dbNotas = SQLiteDatabase.openOrCreateDatabase(fN.getPath(), null);
            dbNotas.execSQL("CREATE TABLE IF NOT EXISTS marcadores (libro INTEGER, cap INTEGER, ver INTEGER, color TEXT, PRIMARY KEY(libro, cap, ver))");
            
            render();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String obtenerColor(int v) {
        Cursor c = dbNotas.rawQuery("SELECT color FROM marcadores WHERE libro="+libroId+" AND cap="+capituloActual+" AND ver="+v, null);
        String res = (c.moveToFirst()) ? c.getString(0) : "";
        c.close();
        return res;
    }

    private void guardarColor(int v, String col) {
        if (col.equals("BORRAR")) {
            dbNotas.execSQL("DELETE FROM marcadores WHERE libro="+libroId+" AND cap="+capituloActual+" AND ver="+v);
        } else {
            dbNotas.execSQL("INSERT OR REPLACE INTO marcadores VALUES ("+libroId+","+capituloActual+","+v+",'"+col+"')");
        }
        render();
    }

    private void mostrarSelectorColores(int vNum) {
        BottomSheetDialog d = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 60, 40, 60);
        layout.setGravity(android.view.Gravity.CENTER);
        
        String[] colors = {"AMARILLO", "AZUL", "VERDE", "BORRAR"};
        int[] codes = {Color.YELLOW, Color.CYAN, Color.GREEN, Color.GRAY};

        for (int i = 0; i < colors.length; i++) {
            final String name = colors[i];
            Button b = new Button(this);
            b.setText(name);
            b.setOnClickListener(v -> { guardarColor(vNum, name); d.dismiss(); });
            layout.addView(b);
        }
        d.setContentView(layout);
        d.show();
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_books) mostrarSelectorLibros();
            else mostrarSelectorVersiones();
            return true;
        });
    }

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> nombres = new ArrayList<>();
        final List<Integer> ids = new ArrayList<>();
        
        for (String lib : ORDEN_LIBROS) {
            Cursor c = dbBiblia.rawQuery("SELECT book_number, long_name FROM books WHERE long_name LIKE '%"+lib+"%' LIMIT 1", null);
            if (c.moveToFirst()) {
                ids.add(c.getInt(0));
                nombres.add(c.getString(1));
            }
            c.close();
        }
        
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        lv.setOnItemClickListener((a, v, p, id) -> {
            libroId = ids.get(p);
            capituloActual = 1;
            d.dismiss();
            mostrarSelectorCapitulos();
        });
        d.setContentView(lv);
        d.show();
    }

    private void mostrarSelectorCapitulos() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number="+libroId, null);
        int max = (c.moveToFirst()) ? c.getInt(0) : 1; c.close();
        
        List<String> caps = new ArrayList<>();
        for(int i=1; i<=max; i++) caps.add("Capítulo " + i);
        
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        lv.setOnItemClickListener((a, v, p, id) -> {
            capituloActual = p + 1;
            prefs.edit().putInt("last_book", libroId).putInt("last_cap", capituloActual).apply();
            d.dismiss();
            render();
        });
        d.setContentView(lv);
        d.show();
    }

    private void mostrarSelectorVersiones() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] vers = {"NVI", "LBLA", "DHHS", "PDT"};
        final String[] files = {"NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3", "PDT.SQLite3"};
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, vers));
        lv.setOnItemClickListener((a,v,p,id) -> {
            versionActual = files[p];
            prefs.edit().putString("last_version", versionActual).apply();
            abrirBiblia();
            d.dismiss();
        });
        d.setContentView(lv);
        d.show();
    }

    private void aplicarTema() {
        findViewById(R.id.rootLayout).setBackgroundColor(modoOscuro ? Color.BLACK : Color.WHITE);
        txtNavAbrev.setTextColor(modoOscuro ? Color.LTGRAY : Color.DKGRAY);
    }

    private void mostrarAjustes() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        TextView t = new TextView(this); t.setText("Tamaño de letra: " + tamanoLetra);
        SeekBar sb = new SeekBar(this); sb.setMax(20); sb.setProgress(tamanoLetra-10);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                tamanoLetra = p + 10;
                t.setText("Tamaño de letra: " + tamanoLetra);
                render();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt("font_size", tamanoLetra).apply();
            }
        });
        
        layout.addView(t);
        layout.addView(sb);
        d.setContentView(layout);
        d.show();
    }
}