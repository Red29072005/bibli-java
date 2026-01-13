package com.biblia.lite;

import android.content.Context;
import android.content.SharedPreferences;
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
    private SharedPreferences prefs;

    // Tu orden personalizado de libros
    private final String[] ORDEN_LIBROS = {
        "Génesis", "Éxodo", "Levítico", "Números", "Deuteronomio", "Josué", "Jueces", "Job",
        "Rut", "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "Salmos", "Proverbios", 
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

        // Memoria (SharedPreferences)
        prefs = getSharedPreferences("BibliaPrefs", Context.MODE_PRIVATE);
        versionActual = prefs.getString("last_version", "NVI'22.SQLite3");
        libroId = prefs.getInt("last_book", 1);
        capituloActual = prefs.getInt("last_cap", 1);

        rvVersiculos = findViewById(R.id.rvVersiculos);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));
        // Padding extra para que no se corte el final
        rvVersiculos.setPadding(0, 0, 0, 200);
        rvVersiculos.setClipToPadding(false);

        // Botones de Navegación (vinculados al XML control_bar)
        try {
            findViewById(R.id.btnAnterior).setOnClickListener(v -> cambiarCapitulo(-1));
            findViewById(R.id.btnSiguiente).setOnClickListener(v -> cambiarCapitulo(1));
        } catch (Exception e) { /* Si no existen en XML no crashea */ }

        setupNavigation();
        abrirBiblia();
    }

    private void cambiarCapitulo(int delta) {
        int nuevoCap = capituloActual + delta;
        if (nuevoCap < 1) return;
        
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
        if (c.moveToFirst() && nuevoCap <= c.getInt(0)) {
            capituloActual = nuevoCap;
            guardarProgreso();
            render();
            rvVersiculos.scrollToPosition(0);
        }
        c.close();
    }

    private void guardarProgreso() {
        prefs.edit().putString("last_version", versionActual)
              .putInt("last_book", libroId).putInt("last_cap", capituloActual).apply();
    }

    private void abrirBiblia() {
        try {
            if (dbBiblia != null) dbBiblia.close();
            if (dbNotas != null) dbNotas.close();

            // 1. Abrir Biblia (Solo lectura)
            File f = forzarCopia(versionActual);
            dbBiblia = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // 2. Abrir Notas (LECTURA Y ESCRITURA para los colores)
            String nNota = versionActual.replace(".SQLite3", ".commentaries.SQLite3");
            try {
                File fn = forzarCopia(nNota);
                // IMPORTANTE: OPEN_READWRITE permite guardar los colores
                dbNotas = SQLiteDatabase.openDatabase(fn.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            } catch (Exception e) { dbNotas = null; }
            
            render();
        } catch (Exception e) {
            Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show();
        }
    }

    private File forzarCopia(String name) throws Exception {
        File f = new File(getFilesDir(), name);
        // Siempre sobrescribimos para evitar corrupción, aunque es un poco más lento
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
            // Título
            String nombreLibro = obtenerNombreLibro(libroId);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(nombreLibro + " " + capituloActual);

            // Consulta Versículos
            Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number = " + libroId + " AND chapter = " + capituloActual + " ORDER BY verse ASC", null);
            
            while (c.moveToNext()) {
                int nV = c.getInt(0);
                String txt = c.getString(1);

                // --- LIMPIEZA VISUAL (Regex) ---
                txt = txt.replaceAll("<[^>]+>", ""); // HTML tags
                txt = txt.replaceAll("\\[\\d+\\]", ""); // [1]
                txt = txt.replaceAll("\\(\\w+\\)", ""); // (a)
                txt = txt.replaceAll("[\\u2460-\\u24FF\\u00A9\\u00AE]", ""); // Circulos, Copyright
                txt = txt.trim();

                // --- LOGICA DE NOTAS ---
                if (tieneNota(nV)) txt += " [#]";

                // --- LOGICA DE COLORES ---
                // Buscamos si hay un color guardado para este versículo
                String colorTag = obtenerColor(nV); // Devuelve "##AZUL" o vacío
                
                listaVersiculos.add(nV + " " + txt + " " + colorTag);
            }
            c.close();

            adapter = new VersiculoAdapter(listaVersiculos, 18, true);
            adapter.setOnItemClickListener(pos -> {
                String fullText = listaVersiculos.get(pos);
                // Extraer numero versículo
                int numV = Integer.parseInt(fullText.split(" ")[0]);
                
                if (fullText.contains("[#]")) {
                     mostrarNota(numV); // Prioridad a la nota
                } else {
                     mostrarSelectorColores(numV); // Si no es nota, abrimos selector de color
                }
            });
            rvVersiculos.setAdapter(adapter);

        } catch (Exception e) {
            Toast.makeText(this, "Error render", Toast.LENGTH_SHORT).show();
        }
    }

    // Método auxiliar para el título
    private String obtenerNombreLibro(int id) {
        String res = "Libro " + id;
        Cursor c = null;
        try {
            c = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number=" + id, null);
            if(c.moveToFirst()) res = c.getString(0);
        } catch(Exception e) {
            try {
                if(c!=null) c.close();
                c = dbBiblia.rawQuery("SELECT long_name FROM books_all WHERE book_number=" + id, null);
                if(c.moveToFirst()) res = c.getString(0);
            } catch(Exception e2){}
        } finally { if(c!=null) c.close(); }
        return res;
    }

    private boolean tieneNota(int v) {
        if (dbNotas == null) return false;
        try {
            // Buscamos cualquier cosa que NO sea un color (marker != 'COLOR')
            Cursor c = dbNotas.rawQuery("SELECT 1 FROM commentaries WHERE book_number=" + libroId + 
                " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v + " AND (marker IS NULL OR marker != 'COLOR')", null);
            boolean ex = c.moveToFirst(); c.close(); return ex;
        } catch (Exception e) { return false; }
    }
    
    private String obtenerColor(int v) {
        if (dbNotas == null) return "";
        String col = "";
        try {
            Cursor c = dbNotas.rawQuery("SELECT text FROM commentaries WHERE book_number=" + libroId + 
                " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v + " AND marker = 'COLOR'", null);
            if (c.moveToFirst()) col = c.getString(0); // Debería ser "##AZUL"
            c.close();
        } catch (Exception e) {}
        return col;
    }

    private void mostrarNota(int v) {
        if (dbNotas == null) return;
        Cursor c = dbNotas.rawQuery("SELECT text FROM commentaries WHERE book_number=" + libroId + 
            " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v + " AND (marker IS NULL OR marker != 'COLOR')", null);
        if (c.moveToFirst()) {
            new AlertDialog.Builder(this)
                .setTitle("Nota v." + v)
                .setMessage(c.getString(0).replaceAll("<[^>]+>", "").trim())
                .setPositiveButton("CERRAR", null).show();
        }
        c.close();
    }

    private void mostrarSelectorColores(int vNum) {
        if(dbNotas == null) {
            Toast.makeText(this, "Marcado no disponible en esta versión", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog d = new BottomSheetDialog(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(30, 50, 30, 50);

        int[] colores = {Color.parseColor("#2196F3"), Color.parseColor("#FFEB3B"), Color.parseColor("#4CAF50"), Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"), Color.LTGRAY};
        String[] nombres = {"AZUL", "AMARILLO", "VERDE", "MORADO", "ROSADO", "BORRAR"};

        for (int i = 0; i < colores.length; i++) {
            final String colName = nombres[i];
            android.view.View circle = new android.view.View(this);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(120, 120);
            params.setMargins(15, 0, 15, 0);
            circle.setLayoutParams(params);
            
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(colores[i]);
            shape.setStroke(2, Color.WHITE);
            circle.setBackground(shape);

            circle.setOnClickListener(v -> {
                guardarMarcaColor(vNum, colName);
                d.dismiss();
                render(); // Recargar para ver cambios
            });
            layout.addView(circle);
        }
        d.setContentView(layout);
        d.show();
    }

    private void guardarMarcaColor(int v, String colorName) {
        try {
            if (colorName.equals("BORRAR")) {
                dbNotas.execSQL("DELETE FROM commentaries WHERE book_number=" + libroId + 
                    " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v + " AND marker='COLOR'");
            } else {
                // INSERT OR REPLACE para guardar el color
                dbNotas.execSQL("DELETE FROM commentaries WHERE book_number=" + libroId + 
                    " AND chapter_number_from=" + capituloActual + " AND verse_number_from=" + v + " AND marker='COLOR'");
                    
                dbNotas.execSQL("INSERT INTO commentaries (book_number, chapter_number_from, verse_number_from, marker, text) " +
                    "VALUES (" + libroId + ", " + capituloActual + ", " + v + ", 'COLOR', '##" + colorName + "')");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar color", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> nombresAMostrar = new ArrayList<>();
        final List<Integer> idsCorrespondientes = new ArrayList<>();

        // Usamos TU lista maestra
        for (String nombreObjetivo : ORDEN_LIBROS) {
            Cursor c = null;
            try {
                // Buscamos coincidencia parcial
                String query = "SELECT book_number, long_name FROM books WHERE long_name LIKE '%" + nombreObjetivo + "%' LIMIT 1";
                c = dbBiblia.rawQuery(query, null);
                
                // Si falla en books, busca en books_all
                if (!c.moveToFirst()) {
                    c.close();
                    query = "SELECT book_number, long_name FROM books_all WHERE long_name LIKE '%" + nombreObjetivo + "%' LIMIT 1";
                    c = dbBiblia.rawQuery(query, null);
                }
                
                if (c.moveToFirst()) {
                    idsCorrespondientes.add(c.getInt(0));
                    nombresAMostrar.add(c.getString(1)); // Muestra el nombre real de la DB
                }
            } catch (Exception e) {
                // Libro no encontrado en esta versión
            } finally { if(c!=null) c.close(); }
        }

        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombresAMostrar));
        lv.setOnItemClickListener((ad, vi, pos, id) -> {
            libroId = idsCorrespondientes.get(pos);
            capituloActual = 1;
            guardarProgreso();
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
            guardarProgreso();
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
            guardarProgreso();
            abrirBiblia();
            d.dismiss();
        });
        d.setContentView(lv); d.show();
    }
}