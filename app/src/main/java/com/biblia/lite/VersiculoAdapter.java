package com.biblia.lite;

import android.graphics.Color;
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
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public VersiculoAdapter(List<String> versiculos, int tamanoTexto, boolean modoNoche) {
        this.versiculos = versiculos;
        this.tamanoTexto = tamanoTexto;
        this.modoNoche = modoNoche;
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
        
        // 1. Detección de Colores
        int colorFondo = Color.TRANSPARENT;
        if (rawText.contains("##AZUL")) colorFondo = Color.parseColor("#332196F3"); // Azul suave
        else if (rawText.contains("##AMARILLO")) colorFondo = Color.parseColor("#33FFEB3B"); // Amarillo suave
        else if (rawText.contains("##VERDE")) colorFondo = Color.parseColor("#334CAF50"); // Verde suave
        else if (rawText.contains("##MORADO")) colorFondo = Color.parseColor("#339C27B0"); // Morado suave
        else if (rawText.contains("##ROSADO")) colorFondo = Color.parseColor("#33E91E63"); // Rosado suave
        
        holder.itemView.setBackgroundColor(colorFondo);

        // 2. Limpieza visual (Quitamos el código ##COLOR para que el usuario no lo vea)
        String textoLimpio = rawText.replaceAll("##[A-Z]+", "");
        
        holder.textView.setText(textoLimpio);
        holder.textView.setTextSize(tamanoTexto);
        holder.textView.setTextColor(modoNoche ? Color.WHITE : Color.BLACK);
        
        // 3. Click Listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() { return versiculos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(@NonNull View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }
}