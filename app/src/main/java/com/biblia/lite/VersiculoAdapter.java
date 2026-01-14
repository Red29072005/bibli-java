package com.biblia.lite;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VersiculoAdapter extends RecyclerView.Adapter<VersiculoAdapter.ViewHolder> {
    private List<String> versiculos;
    private int tamanoTexto;
    private boolean modoNoche;
    private String tipoFuente;
    private OnItemClickListener clickListener;

    public interface OnItemClickListener { void onItemClick(int position); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }

    public VersiculoAdapter(List<String> versiculos, int tamanoTexto, boolean modoNoche, String tipoFuente) {
        this.versiculos = versiculos;
        this.tamanoTexto = tamanoTexto;
        this.modoNoche = modoNoche;
        this.tipoFuente = tipoFuente;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String rawText = versiculos.get(position);
        
        // 1. Manejo de Colores de resaltado
        int colorFondo = Color.TRANSPARENT;
        if (rawText.contains("##AZUL")) colorFondo = Color.parseColor("#442196F3");
        else if (rawText.contains("##AMARILLO")) colorFondo = Color.parseColor("#44FFEB3B");
        else if (rawText.contains("##VERDE")) colorFondo = Color.parseColor("#444CAF50");
        holder.itemView.setBackgroundColor(colorFondo);

        // 2. Formatear Versículo (Número gris y pequeño)
        String textoLimpio = rawText.replaceAll("##[A-Z]+", "").trim();
        
        // Buscamos dónde termina el número (primer espacio)
        int primerEspacio = textoLimpio.indexOf(" ");
        // Si el texto empieza con salto de línea, buscamos el número después
        if (textoLimpio.startsWith("\n")) {
            primerEspacio = textoLimpio.indexOf(" ", textoLimpio.lastIndexOf("\n"));
        }

        SpannableString spannable = new SpannableString(textoLimpio);
        if (primerEspacio > 0) {
            int inicioNum = textoLimpio.startsWith("\n") ? textoLimpio.lastIndexOf("\n") + 1 : 0;
            // Color gris clarito al número
            spannable.setSpan(new ForegroundColorSpan(Color.GRAY), inicioNum, primerEspacio, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Tamaño un poco más pequeño al número
            spannable.setSpan(new RelativeSizeSpan(0.8f), inicioNum, primerEspacio, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.textView.setText(spannable);
        holder.textView.setTextSize(tamanoTexto);
        holder.textView.setTextColor(modoNoche ? Color.WHITE : Color.BLACK);
        holder.textView.setLineSpacing(0, 1.3f); // Interlineado cómodo

        // Fuente
        if (tipoFuente.equals("SERIF")) holder.textView.setTypeface(Typeface.SERIF);
        else if (tipoFuente.equals("MONO")) holder.textView.setTypeface(Typeface.MONOSPACE);
        else holder.textView.setTypeface(Typeface.SANS_SERIF);

        holder.itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onItemClick(position); });
    }

    @Override
    public int getItemCount() { return versiculos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(@NonNull View v) { super(v); textView = v.findViewById(android.R.id.text1); }
    }
}