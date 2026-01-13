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
    private OnItemClickListener listener; // <--- Importante

    // La pieza que faltaba
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
        holder.textView.setText(versiculos.get(position));
        holder.textView.setTextSize(tamanoTexto);
        holder.textView.setTextColor(modoNoche ? Color.WHITE : Color.BLACK);
        
        // Detectar el toque en el versículo
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