package com.biblia.lite;

import android.graphics.Color;
import android.graphics.Typeface;
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
        tv.setPadding(30, 20, 30, 20);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String raw = versiculos.get(position);
        String textoMostrar = raw;
        String colorResaltado = "";

        if (raw.contains(" ##")) {
            String[] parts = raw.split(" ##");
            textoMostrar = parts[0];
            colorResaltado = parts[1];
        }

        holder.tv.setText(textoMostrar);
        holder.tv.setTextSize(tamano);
        holder.tv.setTextColor(modoOscuro ? Color.WHITE : Color.BLACK);

        // Aplicar Color de Resaltado
        if (!colorResaltado.isEmpty()) {
            int alpha = 120;
            if (colorResaltado.equals("AMARILLO")) holder.tv.setBackgroundColor(Color.argb(alpha, 255, 255, 0));
            else if (colorResaltado.equals("AZUL")) holder.tv.setBackgroundColor(Color.argb(alpha, 0, 200, 255));
            else if (colorResaltado.equals("VERDE")) holder.tv.setBackgroundColor(Color.argb(alpha, 0, 255, 0));
        } else {
            holder.tv.setBackgroundColor(Color.TRANSPARENT);
        }

        // Tipo de Fuente
        if (fuente.equals("SERIF")) holder.tv.setTypeface(Typeface.SERIF);
        else if (fuente.equals("MONO")) holder.tv.setTypeface(Typeface.MONOSPACE);
        else holder.tv.setTypeface(Typeface.SANS_SERIF);

        // CLIC CORTO (Notas)
        holder.tv.setOnClickListener(v -> listener.onShortClick(position));
        
        // CLIC LARGO (Marcado)
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
