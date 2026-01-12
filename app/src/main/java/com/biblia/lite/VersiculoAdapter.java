package com.biblia.lite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VersiculoAdapter extends RecyclerView.Adapter<VersiculoAdapter.ViewHolder> {
    private List<String> versiculos;
    private int size;
    private boolean dark;
    private OnAction listener;

    public interface OnAction { void onLongClick(int pos); }

    public VersiculoAdapter(List<String> versiculos, int size, boolean dark, OnAction listener) {
        this.versiculos = versiculos;
        this.size = size;
        this.dark = dark;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText((position + 1) + " " + versiculos.get(position));
        holder.textView.setTextSize(size);
        holder.textView.setTextColor(dark ? 0xFFFFFFFF : 0xFF000000);
        
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(position);
            holder.textView.setBackgroundColor(0x33BB86FC);
            return true;
        });
    }

    @Override
    public int getItemCount() { return versiculos.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
        }
    }
}
