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
    private int fontSize;
    private boolean isDark;

    public VersiculoAdapter(List<String> versiculos, int fontSize, boolean isDark) {
        this.versiculos = versiculos;
        this.fontSize = fontSize;
        this.isDark = isDark;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usamos un layout simple pero controlamos los colores por código
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.id.text1, parent, false);
        if (!(view instanceof TextView)) {
            view = new TextView(parent.getContext());
            view.setPadding(20, 10, 20, 10);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView tv = (TextView) holder.itemView;
        tv.setText((position + 1) + " " + versiculos.get(position));
        tv.setTextSize(fontSize);
        
        // Colores de alto contraste
        tv.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        tv.setBackgroundColor(isDark ? Color.BLACK : Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return versiculos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }
}