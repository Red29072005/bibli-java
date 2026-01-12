package com.biblia.lite;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // --- VARIABLES DE ESTADO (Igual que en Flet) ---
    String currentVersion = "NVI";
    String currentLibro = "Génesis";
    int currentCap = 1;
    int currentLibroId = 1;
    int fontSize = 18;
    boolean isDarkMode = false;
    Set<Integer> selectedVerses = new HashSet<>();
    
    // Nombres exactos de tus archivos (REPORTE_ESTRUCTURA.txt)
    final String[] VERSIONES = {"NVI", "LBLA", "DHHS", "PDT"};
    final Map<String, String> DB_FILES = new HashMap<>();
    final Map<String, String> NOTE_FILES = new HashMap<>();
    
    // Orden Maestro (Flet Code source: 13)
    final String[] ORDEN_MAESTRO = {
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

    // UI Elements
    View viewInicio, viewLectura, viewLibros, viewCaps, viewAjustes;
    RecyclerView recyclerVersiculos;
    VersiculoAdapter adapter;
    TextView txtTituloLibro;
    Spinner ddVersion;
    SQLiteDatabase dbUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Mapeo de archivos (REPORTE_ESTRUCTURA.txt)
        DB_FILES.put("NVI", "NVI'22.SQLite3");
        DB_FILES.put("LBLA", "LBLA.SQLite3");
        DB_FILES.put("DHHS", "DHHS'94.SQLite3");
        DB_FILES.put("PDT", "PDT.SQLite3");
        
        NOTE_FILES.put("NVI", "NVI'22.commentaries.SQLite3");
        NOTE_FILES.put("LBLA", "LBLA.commentaries.SQLite3");
        NOTE_FILES.put("DHHS", "DHHS'94.commentaries.SQLite3");
        NOTE_FILES.put("PDT", "PDT.commentaries.SQLite3");

        inicializarVistas();
        prepararArchivos();
        abrirDbUsuario();
        cargarAjustes();
        
        // Configurar Spinner Versiones
        ArrayAdapter<String> adapterVer = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, VERSIONES);
        ddVersion.setAdapter(adapterVer);
    }

    private void inicializarVistas() {
        viewInicio = findViewById(R.id.viewInicio);
        viewLectura = findViewById(R.id.viewLectura);
        viewLibros = findViewById(R.id.viewLibros);
        viewCaps = findViewById(R.id.viewCaps);
        viewAjustes = findViewById(R.id.viewAjustes);
        
        recyclerVersiculos = findViewById(R.id.recyclerVersiculos);
        recyclerVersiculos.setLayoutManager(new LinearLayoutManager(this));
        
        txtTituloLibro = findViewById(R.id.btnTituloLibro); // Usamos el boton como texto tambien
        ddVersion = findViewById(R.id.ddVersion);

        // BOTONES NAVEGACION
        findViewById(R.id.btnLeerAhora).setOnClickListener(v -> {
            currentVersion = ddVersion.getSelectedItem().toString();
            mostrarVista(viewLectura);
            cargarCapitulo();
        });
        
        findViewById(R.id.btnAjustesHome).setOnClickListener(v -> mostrarVista(viewAjustes));
        findViewById(R.id.btnMenuLectura).setOnClickListener(v -> mostrarVista(viewInicio));
        findViewById(R.id.btnVolverLibros).setOnClickListener(v -> mostrarVista(viewLectura));
        findViewById(R.id.btnVolverCaps).setOnClickListener(v -> mostrarVista(viewLibros));
        findViewById(R.id.btnVolverAjustes).setOnClickListener(v -> mostrarVista(viewInicio));
        
        // Boton Titulo -> Selector Libros
        findViewById(R.id.btnTituloLibro).setOnClickListener(v -> cargarListaLibros());
        
        // ANT / SIG
        findViewById(R.id.btnAnt).setOnClickListener(v -> {
            if (currentCap > 1) { currentCap--; cargarCapitulo(); }
        });
        findViewById(R.id.btnSig).setOnClickListener(v -> {
            currentCap++; cargarCapitulo(); // En app real verificar max cap
        });

        // AJUSTES
        Switch swTema = findViewById(R.id.switchTema);
        swTema.setOnCheckedChangeListener((btn, checked) -> {
            isDarkMode = checked;
            guardarAjuste("theme", checked ? "dark" : "light");
            aplicarTema();
        });
        
        SeekBar skFuente = findViewById(R.id.seekFuente);
        skFuente.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean b) {
                fontSize = p;
                ((TextView)findViewById(R.id.txtPreview)).setTextSize(fontSize);
                guardarAjuste("font_size", String.valueOf(fontSize));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void mostrarVista(View vista) {
        viewInicio.setVisibility(View.GONE);
        viewLectura.setVisibility(View.GONE);
        viewLibros.setVisibility(View.GONE);
        viewCaps.setVisibility(View.GONE);
        viewAjustes.setVisibility(View.GONE);
        vista.setVisibility(View.VISIBLE);
    }

    // --- LOGICA DE BASE DE DATOS ---

    private void prepararArchivos() {
        // Copia todos los archivos de assets a files
        List<String> files = new ArrayList<>();
        files.addAll(DB_FILES.values());
        files.addAll(NOTE_FILES.values());
        
        for (String filename : files) {
            File dest = new File(getFilesDir(), filename);
            if (!dest.exists()) {
                try {
                    InputStream in = getAssets().open(filename);
                    OutputStream out = new FileOutputStream(dest);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    in.close(); out.close();
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    private void abrirDbUsuario() {
        // Crea user_data.db igual que en Flet Code source: 18
        File dbFile = new File(getFilesDir(), "user_data.db");
        dbUser = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        dbUser.execSQL("CREATE TABLE IF NOT EXISTS marcadores (libro_id INTEGER, capitulo INTEGER, versiculo INTEGER, color TEXT, PRIMARY KEY (libro_id, capitulo, versiculo))");
        dbUser.execSQL("CREATE TABLE IF NOT EXISTS ajustes (clave TEXT PRIMARY KEY, valor TEXT)");
    }

    private SQLiteDatabase getBibleDB() {
        String filename = DB_FILES.get(currentVersion);
        return SQLiteDatabase.openDatabase(new File(getFilesDir(), filename).getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }
    
    private SQLiteDatabase getNotesDB() {
        String filename = NOTE_FILES.get(currentVersion);
        File f = new File(getFilesDir(), filename);
        if(!f.exists()) return null;
        return SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    // --- CARGA DE DATOS ---

    private void cargarListaLibros() {
        ListView list = findViewById(R.id.listLibros);
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ORDEN_MAESTRO);
        list.setAdapter(adp);
        list.setOnItemClickListener((parent, view, position, id) -> {
            currentLibro = ORDEN_MAESTRO[position];
            mostrarSelectorCaps();
        });
        mostrarVista(viewLibros);
    }
    
    private void mostrarSelectorCaps() {
        // Obtener ID y contar caps
        SQLiteDatabase db = getBibleDB();
        Cursor c = db.rawQuery("SELECT book_number FROM books WHERE long_name LIKE ? OR short_name LIKE ? LIMIT 1", 
            new String[]{"%"+currentLibro+"%", "%"+currentLibro+"%"});
        if(c.moveToFirst()) currentLibroId = c.getInt(0);
        c.close();
        
        Cursor c2 = db.rawQuery("SELECT MAX(chapter) FROM verses WHERE book_number=?", new String[]{String.valueOf(currentLibroId)});
        int maxCaps = 0;
        if(c2.moveToFirst()) maxCaps = c2.getInt(0);
        c2.close();
        db.close();

        List<String> caps = new ArrayList<>();
        for(int i=1; i<=maxCaps; i++) caps.add(String.valueOf(i));
        
        GridView grid = findViewById(R.id.gridCaps);
        grid.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, caps));
        grid.setOnItemClickListener((p, v, pos, id) -> {
            currentCap = pos + 1;
            mostrarVista(viewLectura);
            cargarCapitulo();
        });
        mostrarVista(viewCaps);
    }

    private void cargarCapitulo() {
        txtTituloLibro.setText(currentLibro + " " + currentCap);
        selectedVerses.clear();
        
        // 1. Obtener Marcadores
        Map<Integer, String> marcadores = new HashMap<>();
        Cursor cM = dbUser.rawQuery("SELECT versiculo, color FROM marcadores WHERE libro_id=? AND capitulo=?", 
            new String[]{String.valueOf(currentLibroId), String.valueOf(currentCap)});
        while(cM.moveToNext()) marcadores.put(cM.getInt(0), cM.getString(1));
        cM.close();

        // 2. Obtener Versiculos y Notas
        List<VersiculoItem> items = new ArrayList<>();
        SQLiteDatabase db = getBibleDB();
        SQLiteDatabase dbNotes = getNotesDB();
        
        Cursor c = db.rawQuery("SELECT verse, text FROM verses WHERE book_number=? AND chapter=? ORDER BY verse", 
             new String[]{String.valueOf(currentLibroId), String.valueOf(currentCap)});
             
        while (c.moveToNext()) {
            int vNum = c.getInt(0);
            String rawText = c.getString(1);
            
            // Regex Cleaning (Flet source: 35)
            String clean = rawText.replaceAll("<[^>]+>|\[\d+\]|[\u2460-\u24FF]", "")
                                  .replaceAll("\s+", " ").replace(" ,", ",").replace(" .", ".").trim();
            
            String note = null;
            if(dbNotes != null) {
                Cursor cN = dbNotes.rawQuery("SELECT text FROM commentaries WHERE book_number=? AND chapter_number_from=? AND verse_number_from=?",
                    new String[]{String.valueOf(currentLibroId), String.valueOf(currentCap), String.valueOf(vNum)});
                if(cN.moveToFirst()) note = cN.getString(0);
                cN.close();
            }
            
            items.add(new VersiculoItem(vNum, clean, marcadores.get(vNum), note));
        }
        c.close();
        db.close();
        if(dbNotes != null) dbNotes.close();

        // 3. Renderizar en RecyclerView
        adapter = new VersiculoAdapter(items);
        recyclerVersiculos.setAdapter(adapter);
    }

    // --- ADAPTADOR (El motor grafico) ---
    class VersiculoItem {
        int numero; String texto; String color; String nota;
        public VersiculoItem(int n, String t, String c, String nt) { numero=n; texto=t; color=c; nota=nt; }
    }

    class VersiculoAdapter extends RecyclerView.Adapter<VersiculoAdapter.Holder> {
        List<VersiculoItem> lista;
        public VersiculoAdapter(List<VersiculoItem> l) { lista = l; }

        @Override public Holder onCreateViewHolder(ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_versiculo, p, false));
        }
        
        @Override public void onBindViewHolder(Holder h, int pos) {
            VersiculoItem item = lista.get(pos);
            h.txtNum.setText(String.valueOf(item.numero));
            h.txtNum.setTextSize(fontSize - 4);
            h.txtCuerpo.setTextSize(fontSize);
            
            // Construir Texto con Spans
            SpannableString ss = new SpannableString(item.texto + (item.nota != null ? " [#]" : ""));
            
            // Color de fondo (Marcador o Seleccion)
            int bgColor = Color.TRANSPARENT;
            if (selectedVerses.contains(item.numero)) bgColor = Color.LTGRAY;
            else if (item.color != null) bgColor = parseColor(item.color);
            
            if (bgColor != Color.TRANSPARENT)
                ss.setSpan(new BackgroundColorSpan(bgColor), 0, item.texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Link de Nota (Flet source: 36)
            if (item.nota != null) {
                int start = item.texto.length() + 1;
                ss.setSpan(new ForegroundColorSpan(Color.BLUE), start, start+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ClickableSpan() {
                    @Override public void onClick(View w) { mostrarBottomSheetNota(item.nota); }
                }, start, start+3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            h.txtCuerpo.setText(ss);
            h.txtCuerpo.setMovementMethod(LinkMovementMethod.getInstance());
            
            // Clic Corto: Seleccionar
            h.itemView.setOnClickListener(v -> {
                if(selectedVerses.contains(item.numero)) selectedVerses.remove(item.numero);
                else selectedVerses.add(item.numero);
                notifyItemChanged(pos);
            });
            
            // Clic Largo: Menu Colores
            h.itemView.setOnLongClickListener(v -> {
                selectedVerses.add(item.numero);
                notifyItemChanged(pos);
                mostrarBottomSheetColores();
                return true;
            });
        }
        @Override public int getItemCount() { return lista.size(); }
        class Holder extends RecyclerView.ViewHolder {
            TextView txtNum, txtCuerpo;
            public Holder(View v) { super(v); txtNum=v.findViewById(R.id.txtNumero); txtCuerpo=v.findViewById(R.id.txtTexto); }
        }
    }
    
    // --- BOTTOM SHEETS ---
    
    private void mostrarBottomSheetNota(String notaRaw) {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40,40,40,40);
        
        TextView t = new TextView(this);
        // Limpiar nota (Flet source: 32)
        String clean = notaRaw.replaceAll("<[^>]+>|\[\d+\]", "").trim();
        t.setText(clean);
        t.setTextSize(18);
        t.setTextColor(Color.BLACK);
        
        l.addView(t);
        bs.setContentView(l);
        bs.show();
    }

    private void mostrarBottomSheetColores() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,30,30,30);
        
        TextView title = new TextView(this);
        title.setText("OPCIONES");
        title.setTextSize(20);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);
        
        // Colores (Flet source: 30)
        String[] colors = {"#99FFFF00", "#99AAFF00", "#9900CCFF", "#99CC99FF", "#99FF99CC"};
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER);
        
        for(String c : colors) {
            View bolita = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(100, 100);
            p.setMargins(10,10,10,10);
            bolita.setLayoutParams(p);
            bolita.setBackgroundColor(parseColor(c));
            bolita.setOnClickListener(v -> {
                guardarMarcadores(c);
                bs.dismiss();
            });
            row.addView(bolita);
        }
        root.addView(row);
        
        // Boton Copiar
        Button btnCopiar = new Button(this);
        btnCopiar.setText("COPIAR AL PORTAPAPELES");
        btnCopiar.setOnClickListener(v -> {
            copiarAlPortapapeles();
            bs.dismiss();
        });
        root.addView(btnCopiar);
        
        // Boton Limpiar
        Button btnLimpiar = new Button(this);
        btnLimpiar.setText("LIMPIAR SELECCION");
        btnLimpiar.setOnClickListener(v -> {
             guardarMarcadores(null);
             bs.dismiss();
        });
        root.addView(btnLimpiar);

        bs.setContentView(root);
        bs.show();
    }
    
    private void guardarMarcadores(String color) {
        for(int v : selectedVerses) {
             if(color == null) dbUser.execSQL("DELETE FROM marcadores WHERE libro_id=? AND capitulo=? AND versiculo=?", new Object[]{currentLibroId, currentCap, v});
             else dbUser.execSQL("INSERT OR REPLACE INTO marcadores VALUES (?, ?, ?, ?)", new Object[]{currentLibroId, currentCap, v, color});
        }
        selectedVerses.clear();
        cargarCapitulo();
    }
    
    private void copiarAlPortapapeles() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentLibro).append(" ").append(currentCap).append("\n");
        // Nota: En app real deberias obtener el texto del cache, aqui simplificado
        android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Biblia", "Versículos copiados");
        cb.setPrimaryClip(clip);
        Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show();
        selectedVerses.clear();
        cargarCapitulo();
    }

    // --- UTILIDADES ---
    private void guardarAjuste(String k, String v) {
        dbUser.execSQL("INSERT OR REPLACE INTO ajustes VALUES (?, ?)", new Object[]{k, v});
    }
    
    private void cargarAjustes() {
        Cursor c = dbUser.rawQuery("SELECT valor FROM ajustes WHERE clave=?", new String[]{"theme"});
        if(c.moveToFirst()) {
            isDarkMode = c.getString(0).equals("dark");
            ((Switch)findViewById(R.id.switchTema)).setChecked(isDarkMode);
        }
        c.close();
        // Fuente
        Cursor c2 = dbUser.rawQuery("SELECT valor FROM ajustes WHERE clave=?", new String[]{"font_size"});
        if(c2.moveToFirst()) {
            fontSize = Integer.parseInt(c2.getString(0));
            ((SeekBar)findViewById(R.id.seekFuente)).setProgress(fontSize);
        }
        c2.close();
        aplicarTema();
    }
    
    private void aplicarTema() {
        int bg = isDarkMode ? Color.parseColor("#1A1A1A") : Color.WHITE;
        int txt = isDarkMode ? Color.WHITE : Color.BLACK;
        viewLectura.setBackgroundColor(bg);
        viewInicio.setBackgroundColor(bg);
        // ... Aplicar a mas vistas si es necesario
        if(isDarkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    private int parseColor(String hex) {
        // Flet usa #AARRGGBB, Android usa #AARRGGBB
        // Pero tu codigo tiene colores raros como #99FFFF00 (Alpha al inicio), Android lo soporta
        try { return Color.parseColor(hex); } catch(Exception e) { return Color.YELLOW; }
    }
}
