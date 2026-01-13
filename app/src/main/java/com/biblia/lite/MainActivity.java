package com.biblia.lite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
            if (dbBiblia != null) dbBiblia.close();
            if (dbNotas != null) dbNotas.close();

            // COPIA FORZADA: Esto garantiza que el archivo en el móvil sea igual al del PC
            File f = forzarCopia(versionActual);
            dbBiblia = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // Copia de Notas
            String nNota = versionActual.replace(".SQLite3", ".commentaries.SQLite3");
            try {
                File fn = forzarCopia(nNota);
                dbNotas = SQLiteDatabase.openDatabase(fn.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            } catch (Exception e) { dbNotas = null; } // Si no hay notas, no pasa nada
            
            render();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar la Biblia", Toast.LENGTH_SHORT).show();
        }
    }

    private File forzarCopia(String name) throws Exception {
        File f = new File(getFilesDir(), name);
        InputStream is = getAssets().open(name);
        FileOutputStream os = new FileOutputStream(f);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) > 0) os.write(buffer, 0, read);
        os.close(); is.close();
        return f;
    }

    private void render() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        try {
            // 1. Obtener Nombre del Libro (Prueba books, si falla usa books_all)
            String nombreLibro = "Libro " + libroId;
            Cursor cb = null;
            try {
                cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number = " + libroId, null);
                if (cb.moveToFirst()) nombreLibro = cb.getString(0);
            } catch (Exception e) {
                cb = dbBiblia.rawQuery("SELECT long_name FROM books_all WHERE book_number = " + libroId, null);
                if (cb.moveToFirst()) nombreLibro = cb.getString(0);
            } finally { if(cb != null) cb.close(); }
            
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(nombreLibro + " " + capituloActual);

            // 2. Cargar Versículos (book_number es NUMERIC en todas)
            Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number = " + libroId + " AND chapter = " + capituloActual + " ORDER BY verse ASC", null);
            
            while (c.moveToNext()) {
                int nV = c.getInt(0);
                String txt = c.getString(1).replaceAll("<[^>]+>", "").trim();
                
                if (tieneNota(nV)) txt += " [#]";
                listaVersiculos.add(nV + " " + txt);
            }
            c.close();

            adapter = new VersiculoAdapter(listaVersiculos, 18, true);
            adapter.setOnItemClickListener(new VersiculoAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int pos) {
                    String vText = listaVersiculos.get(pos);
                    if (vText.contains("[#]")) {
                        // Extraemos el número del versículo (lo primero antes del espacio)
                        String[] partes = vText.split(" ");
                        try {
                            int num = Integer.parseInt(partes[0]);
                            mostrarNota(num);
                        } catch (Exception e) { /* error de parseo */ }
                    }
                }
            });
            rvVersiculos.setAdapter(adapter);

        } catch (Exception e) {
            Toast.makeText(this, "Error al mostrar texto", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean tieneNota(int v) {
        if (dbNotas == null) return false;
        try {
            Cursor c = dbNotas.rawQuery("SELECT 1 FROM commentaries WHERE book_number=" + libroId + " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v, null);
            boolean ex = c.moveToFirst(); c.close(); return ex;
        } catch (Exception e) { return false; }
    }

    private void mostrarNota(int v) {
        if (dbNotas == null) return;
        Cursor c = dbNotas.rawQuery("SELECT text FROM commentaries WHERE book_number=" + libroId + " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v, null);
        if (c.moveToFirst()) {
            new AlertDialog.Builder(this)
                .setTitle("Nota v." + v)
                .setMessage(c.getString(0).replaceAll("<[^>]+>", "").trim())
                .setPositiveButton("CERRAR", null)
                .show();
        }
        c.close();
    }

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> n = new ArrayList<>(); final List<Integer> ids = new ArrayList<>();
        
        // Intentar books, si falla books_all
        Cursor c = null;
        try {
            c = dbBiblia.rawQuery("SELECT book_number, long_name FROM books ORDER BY book_number", null);
        } catch (Exception e) {
            c = dbBiblia.rawQuery("SELECT book_number, long_name FROM books_all ORDER BY book_number", null);
        }

        while(c != null && c.moveToNext()){
            ids.add(c.getInt(0));
            n.add(c.getString(1));
        }
        if(c != null) c.close();

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, n));
        lv.setOnItemClickListener((ad,vi,pos,id) -> {
            libroId = ids.get(pos);
            d.dismiss();
            mostrarSelectorCapitulos();
        });
        d.setContentView(lv); d.show();
    }

    private void mostrarSelectorCapitulos() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> caps = new ArrayList<>();
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
        int max = (c.moveToFirst()) ? c.getInt(0) : 1; c.close();
        for(int i=1; i<=max; i++) caps.add("Capítulo " + i);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        lv.setOnItemClickListener((ad,vi,pos,id) -> {
            capituloActual = pos+1;
            d.dismiss();
            render();
        });
        d.setContentView(lv); d.show();
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_books) mostrarSelectorLibros();
            else mostrarSelectorVersiones();
            return true;
        });
    }

    private void mostrarSelectorVersiones() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        String[] n = {"NVI", "LBLA", "DHHS", "PDT"};
        final String[] a = {"NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3", "PDT.SQLite3"};
        ListView lv = new ListView(this);
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, n));
        lv.setOnItemClickListener((ad,vi,pos,id) -> {
            versionActual = a[pos];
            libroId=1; capituloActual=1;
            abrirBiblia();
            d.dismiss();
        });
        d.setContentView(lv); d.show();
    }
}