package com.biblia.lite;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VersiculoAdapter extends RecyclerView.Adapter<VersiculoAdapter.ViewHolder> {
    private List<String> versiculos;
    private int tamano;
    private boolean modoOscuro;
    private String fuente;
    private OnVersiculoClickListener listener;

    public interface OnVersiculoClickListener {
        void onShortClick(int position);
        void onLongClick(int position);
    }

    public VersiculoAdapter(List<String> versiculos, int tamano, boolean modoOscuro, String fuente, OnVersiculoClickListener listener) {
        this.versiculos = versiculos;
        this.tamano = tamano;
        this.modoOscuro = modoOscuro;
        this.fuente = fuente;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(40, 5, 40, 5); // Un poco más de margen lateral
        tv.setLineSpacing(0, 1.1f); // Mejora la lectura entre líneas
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String raw = versiculos.get(position);
        String textoMostrar = raw;
        String colorResaltado = "";

        // 1. Separar el color de resaltado si existe
        if (raw.contains(" ##")) {
            String[] parts = raw.split(" ##");
            textoMostrar = parts[0];
            colorResaltado = parts[1];
        }

        // 2. Lógica para destacar el número (Spannable)
        SpannableString ss = new SpannableString(textoMostrar);
        try {
            // IMPORTANTE: Buscamos el espacio especial \u00A0 que pusimos en el MainActivity
            int primerEspacio = textoMostrar.indexOf("\u00A0"); 
            if (primerEspacio > 0) {
                int colorNumero = modoOscuro ? Color.parseColor("#64B5F6") : Color.parseColor("#1976D2");
                ss.setSpan(new ForegroundColorSpan(colorNumero), 0, primerEspacio, 0);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, primerEspacio, 0);
                ss.setSpan(new RelativeSizeSpan(0.85f), 0, primerEspacio, 0);
            }
        } catch (Exception e) { e.printStackTrace(); }

        holder.tv.setText(ss);
        holder.tv.setTextSize(tamano);
        holder.tv.setTextColor(modoOscuro ? Color.parseColor("#DDDDDD") : Color.parseColor("#333333"));

        // 3. --- LÓGICA DE FUENTE Y ESTILO (Aquí va lo que me preguntaste) ---
        
        // Primero definimos la familia (SANS, SERIF o MONO)
        Typeface baseTf;
        if (fuente.equals("SERIF")) baseTf = Typeface.SERIF;
        else if (fuente.equals("MONO")) baseTf = Typeface.MONOSPACE;
        else baseTf = Typeface.SANS_SERIF;

        // Segundo definimos el estilo (Cursiva si es encabezado, sino Normal)
        int estilo = Typeface.NORMAL;
        if (textoMostrar.startsWith("\n(")) { 
            estilo = Typeface.ITALIC;
        }

        // Aplicamos ambos al TextView
        holder.tv.setTypeface(Typeface.create(baseTf, estilo));

        // 4. Aplicar Color de Resaltado (Fondo)
        if (!colorResaltado.isEmpty()) {
            int alpha = 110; 
            if (colorResaltado.equals("AMARILLO")) holder.tv.setBackgroundColor(Color.argb(alpha, 255, 255, 0));
            else if (colorResaltado.equals("AZUL")) holder.tv.setBackgroundColor(Color.argb(alpha, 0, 200, 255));
            else if (colorResaltado.equals("VERDE")) holder.tv.setBackgroundColor(Color.argb(alpha, 0, 255, 0));
            else if (colorResaltado.equals("ROSA")) holder.tv.setBackgroundColor(Color.argb(alpha, 255, 100, 200));
        } else {
            holder.tv.setBackgroundColor(Color.TRANSPARENT);
        }

        // 5. Listeners
        holder.tv.setOnClickListener(v -> listener.onShortClick(position));
        holder.tv.setOnLongClickListener(v -> {
            listener.onLongClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() { return versiculos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        public ViewHolder(View v) { super(v); tv = (TextView) v; }
    }
}