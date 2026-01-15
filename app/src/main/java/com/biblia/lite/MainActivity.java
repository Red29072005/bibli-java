package com.biblia.lite;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
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
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvVersiculos;
    private List<String> listaVersiculos = new ArrayList<>();
    private SQLiteDatabase dbBiblia, dbNotas, dbUser;
    private String versionActual, tipoFuente;
    private int libroId, capituloActual, tamanoLetra;
    private boolean modoOscuro;
    private SharedPreferences prefs;
    private TextView txtNavAbrev;

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
        cargarAjustes();

        rvVersiculos = findViewById(R.id.rvVersiculos);
        txtNavAbrev = findViewById(R.id.txtNavAbrev);
        rvVersiculos.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAnterior).setOnClickListener(v -> cambiarCapitulo(-1));
        findViewById(R.id.btnSiguiente).setOnClickListener(v -> cambiarCapitulo(1));

        setupNavigation();
        abrirBasesDeDatos();
        aplicarTema(); 
        render();
    }

    private void cargarAjustes() {
        versionActual = prefs.getString("last_version", "NVI'22.SQLite3");
        libroId = prefs.getInt("last_book", 1);
        capituloActual = prefs.getInt("last_cap", 1);
        tamanoLetra = prefs.getInt("font_size", 18);
        tipoFuente = prefs.getString("font_type", "SANS");
        modoOscuro = prefs.getBoolean("dark_mode", true);
    }

    private void abrirBasesDeDatos() {
        try {
            File f = new File(getFilesDir(), versionActual);
            if (!f.exists()) copiarAsset(versionActual, f);
            dbBiblia = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);

            String fileNotas = versionActual.replace(".SQLite3", ".commentaries.SQLite3");
            File fN = new File(getFilesDir(), fileNotas);
            if (!fN.exists()) try { copiarAsset(fileNotas, fN); } catch(Exception e){}
            dbNotas = fN.exists() ? SQLiteDatabase.openDatabase(fN.getPath(), null, SQLiteDatabase.OPEN_READONLY) : null;

            dbUser = SQLiteDatabase.openOrCreateDatabase(new File(getFilesDir(), "user_marks.db").getPath(), null);
            dbUser.execSQL("CREATE TABLE IF NOT EXISTS marcadores (l INTEGER, c INTEGER, v INTEGER, color TEXT, PRIMARY KEY(l,c,v))");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void aplicarTema() {
        int fondo = modoOscuro ? Color.BLACK : Color.WHITE;
        int texto = modoOscuro ? Color.parseColor("#CCCCCC") : Color.parseColor("#444444"); // Gris suave
        int barraControl = modoOscuro ? Color.parseColor("#1A1A1A") : Color.parseColor("#F5F5F5");
        int azul = Color.parseColor("#2196F3");

        findViewById(R.id.rootLayout).setBackgroundColor(fondo);
        findViewById(R.id.control_bar).setBackgroundColor(barraControl);
        txtNavAbrev.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);

        ((Button)findViewById(R.id.btnAnterior)).setTextColor(azul);
        ((Button)findViewById(R.id.btnSiguiente)).setTextColor(azul);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setBackgroundColor(barraControl);
        int colorIconos = modoOscuro ? Color.WHITE : Color.BLACK;
        nav.setItemIconTintList(android.content.res.ColorStateList.valueOf(colorIconos));
        nav.setItemTextColor(android.content.res.ColorStateList.valueOf(colorIconos));
    }

    private void render() {
        if (dbBiblia == null) return;
        listaVersiculos.clear();
        
        // Obtenemos el nombre del libro actual para saber si es poético
        String nombreLibro = "";
        Cursor cb = dbBiblia.rawQuery("SELECT long_name FROM books WHERE book_number=" + libroId, null);
        if (cb.moveToFirst()) nombreLibro = cb.getString(0).toLowerCase();
        cb.close();

        txtNavAbrev.setText(obtenerAbrev(libroId) + " " + capituloActual);

        // Identificamos si es un libro que requiere saltos de línea en cada etiqueta <t>
        boolean esLibroPoetico = nombreLibro.contains("salmo") || 
                                nombreLibro.contains("proverbio") || 
                                nombreLibro.contains("cantar") || 
                                nombreLibro.contains("lamentaciones") ||
                                nombreLibro.contains("job");

        Cursor c = dbBiblia.rawQuery("SELECT verse, text FROM verses WHERE book_number=" + libroId + " AND chapter=" + capituloActual + " ORDER BY verse ASC", null);
        while (c.moveToNext()) {
            int nV = c.getInt(0);
            String raw = c.getString(1);

            // 1. Manejo de Encabezados (Salmos): Salto y paréntesis
            raw = raw.replace("<e>", "\n(").replace("</e>", ")\n");

            // 2. Manejo de Párrafos/Diálogos: Salto real
            raw = raw.replace("<pb/>", "\n");

            // 3. Manejo de Poesía (<t>): Sensible al tipo de libro
            if (esLibroPoetico) {
                // En Salmos/Prov, cada <t> es una línea nueva
                raw = raw.replace("<t>", "\n").replace("</t>", "");
            } else {
                // En libros narrativos (como Amós o Juan), el <t> es solo un espacio
                raw = raw.replace("<t>", " ").replace("</t>", " ");
            }

            // 4. Limpieza de etiquetas y basura visual (notas al pie, símbolos)
            String t = raw.replaceAll("<[^>]+>", "")
                        .replaceAll("\\[\\d+\\]", "")
                        .replaceAll("[\\u24D0-\\u24E9\\u24B6-\\u24CF\\u00AE\\u00A9]", "")
                        .replaceAll("\\s+", " ") // Quita dobles espacios resultantes
                        .trim();

            // 5. Icono de notas (ⓘ)
            String notaIcon = "";
            if (dbNotas != null) {
                Cursor cN = dbNotas.rawQuery("SELECT text FROM commentaries WHERE book_number="+libroId+" AND chapter_number_from="+capituloActual+" AND verse_number_from="+nV, null);
                if (cN.moveToFirst()) notaIcon = " ⓘ";
                cN.close();
            }

            // 6. Color de resaltado
            String color = obtenerColorUser(nV);
            
            // Usamos \u00A0 (espacio de no ruptura) para que el número y el texto siempre estén pegados
            listaVersiculos.add(nV + "\u00A0" + t + notaIcon + (color.isEmpty() ? "" : " ##" + color));
        }
        c.close();

        // Actualizamos el RecyclerView
        VersiculoAdapter adapter = new VersiculoAdapter(listaVersiculos, tamanoLetra, modoOscuro, tipoFuente, new VersiculoAdapter.OnVersiculoClickListener() {
            @Override public void onShortClick(int position) { verNota(position); }
            @Override public void onLongClick(int position) { mostrarSelectorColoresPorPosicion(position); }
        });
        rvVersiculos.setAdapter(adapter);
    }

    private void verNota(int pos) {
        if (dbNotas == null) return;
        try {
            String line = listaVersiculos.get(pos);
            // Buscamos el número antes del espacio especial \u00A0
            String vStr = line.split("\u00A0")[0].replaceAll("[^0-9]", "");
            int vNum = Integer.parseInt(vStr);

            Cursor c = dbNotas.rawQuery("SELECT text FROM commentaries WHERE book_number="+libroId+" AND chapter_number_from="+capituloActual+" AND verse_number_from="+vNum, null);
            if (c.moveToFirst()) {
                BottomSheetDialog d = new BottomSheetDialog(this);
                TextView tv = new TextView(this);
                tv.setText(c.getString(0).replaceAll("<[^>]+>", "").trim());
                tv.setPadding(60, 60, 60, 60);
                tv.setTextSize(17);
                tv.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);
                ScrollView s = new ScrollView(this); s.addView(tv);
                s.setBackgroundColor(modoOscuro ? Color.parseColor("#1A1A1A") : Color.WHITE);
                d.setContentView(s); d.show();
            }
            c.close();
        } catch (Exception e) {}
    }

    private void mostrarAjustes() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 50, 60, 70);
        root.setBackgroundColor(modoOscuro ? Color.parseColor("#121212") : Color.WHITE);

        // --- TÍTULO SECCIÓN FUENTE ---
        TextView tFont = new TextView(this);
        tFont.setText("Tipo de letra:");
        tFont.setTextColor(modoOscuro ? Color.GRAY : Color.DKGRAY);
        tFont.setPadding(0, 20, 0, 10);
        root.addView(tFont);

        // --- GRUPO DE OPCIONES (RadioGroup) ---
        RadioGroup rg = new RadioGroup(this);
        String[] fuentes = {"SANS", "SERIF", "MONO"};
        String[] etiquetas = {"Moderna (Sans)", "Clásica (Serif)", "Sistema (Mono)"};

        for (int i = 0; i < fuentes.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(etiquetas[i]);
            rb.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);
            final String fVal = fuentes[i];
            rb.setChecked(tipoFuente.equals(fVal));
            
            rb.setOnClickListener(v -> {
                tipoFuente = fVal;
                prefs.edit().putString("font_type", fVal).apply();
                render();
                // No cerramos el diálogo para que el usuario vea el cambio en tiempo real
            });
            rg.addView(rb);
        }
        root.addView(rg);

        // --- SEPARADOR ---
        View separator = new View(this);
        separator.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        separator.setBackgroundColor(Color.GRAY);
        root.addView(separator);

        // --- MODO OSCURO (Tu código actual) ---
        CheckBox cb = new CheckBox(this);
        cb.setText("Modo Oscuro");
        cb.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);
        cb.setChecked(modoOscuro);
        cb.setOnCheckedChangeListener((v, isChecked) -> {
            modoOscuro = isChecked;
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            aplicarTema(); 
            render(); 
            d.dismiss(); // Al cambiar el tema completo es mejor cerrar y refrescar
        });
        root.addView(cb);

        // --- TAMAÑO DE LETRA (Tu código actual) ---
        TextView tSize = new TextView(this);
        tSize.setText("\nTamaño de letra: " + tamanoLetra);
        tSize.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);
        root.addView(tSize);

        SeekBar sb = new SeekBar(this);
        sb.setMax(20);
        sb.setProgress(tamanoLetra - 12);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean b) {
                tamanoLetra = p + 12;
                tSize.setText("\nTamaño de letra: " + tamanoLetra);
                prefs.edit().putInt("font_size", tamanoLetra).apply();
                render();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(sb);

        d.setContentView(root);
        d.show();
    }    

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_books) mostrarSelectorLibros();
            else if (id == R.id.nav_versions) mostrarSelectorVersiones();
            else if (id == R.id.nav_settings) mostrarAjustes();
            return true;
        });
    }

    private void cambiarCapitulo(int delta) {
        if (dbBiblia == null) return;

        int nCap = capituloActual + delta;

        // 1. Obtener el máximo de capítulos del libro actual
        Cursor c = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
        int maxCapActual = (c.moveToFirst()) ? c.getInt(0) : 1;
        c.close();

        if (nCap < 1) {
            // --- IR AL LIBRO ANTERIOR ---
            // Buscamos el ID del libro que esté inmediatamente antes del actual
            Cursor cPrevLib = dbBiblia.rawQuery("SELECT book_number FROM books WHERE book_number < " + libroId + " ORDER BY book_number DESC LIMIT 1", null);
            
            if (cPrevLib.moveToFirst()) {
                libroId = cPrevLib.getInt(0);
                // Al ir atrás, queremos el ÚLTIMO capítulo del libro anterior
                Cursor cLastCap = dbBiblia.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=" + libroId, null);
                capituloActual = (cLastCap.moveToFirst()) ? cLastCap.getInt(0) : 1;
                cLastCap.close();
            }
            cPrevLib.close();

        } else if (nCap > maxCapActual) {
            // --- IR AL SIGUIENTE LIBRO ---
            // Buscamos el ID del libro que esté inmediatamente después del actual
            Cursor cNextLib = dbBiblia.rawQuery("SELECT book_number FROM books WHERE book_number > " + libroId + " ORDER BY book_number ASC LIMIT 1", null);
            
            if (cNextLib.moveToFirst()) {
                libroId = cNextLib.getInt(0);
                capituloActual = 1; // Empezamos en el primer capítulo
            }
            cNextLib.close();

        } else {
            // --- CAMBIO NORMAL (Mismo libro) ---
            capituloActual = nCap;
        }

        // 2. Guardar progreso, refrescar pantalla y subir al inicio
        prefs.edit().putInt("last_book", libroId).putInt("last_cap", capituloActual).apply();
        render();
        rvVersiculos.scrollToPosition(0);
    }

    private String obtenerAbrev(int id) {
        Cursor c = dbBiblia.rawQuery("SELECT short_name FROM books WHERE book_number=" + id, null);
        String res = c.moveToFirst() ? c.getString(0).toUpperCase() : "LIB";
        c.close(); return res;
    }

    private void guardarColor(int v, String col) {
        if (col.equals("BORRAR")) dbUser.execSQL("DELETE FROM marcadores WHERE l="+libroId+" AND c="+capituloActual+" AND v="+v);
        else dbUser.execSQL("INSERT OR REPLACE INTO marcadores VALUES ("+libroId+","+capituloActual+","+v+",'"+col+"')");
    }

    private String obtenerColorUser(int v) {
        Cursor c = dbUser.rawQuery("SELECT color FROM marcadores WHERE l="+libroId+" AND c="+capituloActual+" AND v="+v, null);
        String res = c.moveToFirst() ? c.getString(0) : "";
        c.close(); return res;
    }

    private void copiarAsset(String name, File dest) throws Exception {
        InputStream is = getAssets().open(name);
        FileOutputStream os = new FileOutputStream(dest);
        byte[] b = new byte[2048]; int l;
        while((l=is.read(b))>0) os.write(b,0,l);
        os.close(); is.close();
    }

    private void mostrarSelectorColoresPorPosicion(int pos) {
        String line = listaVersiculos.get(pos).replace("\n", "").trim();
        int vNum = Integer.parseInt(line.split(" ")[0].replaceAll("[^0-9]", ""));
        
        BottomSheetDialog d = new BottomSheetDialog(this);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setPadding(50, 80, 50, 80); l.setGravity(Gravity.CENTER);
        
        // Colores con transparencia (Alpha 120 / 47%) para que se vea la letra
        int[] colors = {0x78FFFF00, 0x7800FFFF, 0x7800FF00, 0x78FF00FF, Color.LTGRAY};
        String[] names = {"AMARILLO", "AZUL", "VERDE", "ROSA", "BORRAR"};
        
        for (int i = 0; i < colors.length; i++) {
            final String name = names[i];
            View circle = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(110, 110); p.setMargins(20, 0, 20, 0);
            circle.setLayoutParams(p);
            GradientDrawable gd = new GradientDrawable(); 
            gd.setShape(GradientDrawable.OVAL); 
            gd.setColor(colors[i]);
            circle.setBackground(gd);
            circle.setOnClickListener(v -> { guardarColor(vNum, name); d.dismiss(); render(); });
            l.addView(circle);
        }
        scroll.addView(l);
        d.setContentView(scroll); d.show();
    }

    private void mostrarSelectorLibros() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        ListView lv = new ListView(this);
        List<String> nombres = new ArrayList<>();
        final List<Integer> ids = new ArrayList<>();
        for (String lib : ORDEN_LIBROS) {
            Cursor c = dbBiblia.rawQuery("SELECT book_number, long_name FROM books WHERE long_name LIKE '%"+lib+"%' LIMIT 1", null);
            if (c.moveToFirst()) { ids.add(c.getInt(0)); nombres.add(c.getString(1)); }
            c.close();
        }
        lv.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        lv.setOnItemClickListener((a, v, p, id) -> {
            libroId = ids.get(p); capituloActual = 1;
            prefs.edit().putInt("last_book", libroId).putInt("last_cap", 1).apply();
            d.dismiss(); mostrarSelectorCapitulos();
        });
        d.setContentView(lv); d.show();
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
            prefs.edit().putInt("last_cap", capituloActual).apply();
            d.dismiss(); render();
        });
        d.setContentView(lv); d.show();
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
            abrirBasesDeDatos(); d.dismiss(); render();
        });
        d.setContentView(lv); d.show();
    }
}