package com.example.myapplicationf.Adapters;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.TextView;



import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;



import com.example.myapplicationf.Models.HistorialRuta;

import com.example.myapplicationf.R;

import com.google.firebase.Timestamp;



import java.text.SimpleDateFormat;

import java.util.List;

import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {
    private List<HistorialRuta> historialList;
    public HistorialAdapter(List<HistorialRuta> historialList) {
        this.historialList = historialList;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_ruta, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        HistorialRuta ruta = historialList.get(position);
        String origenLabel = holder.itemView.getContext().getString(R.string.historial_origen_label);
        String destinoLabel = holder.itemView.getContext().getString(R.string.historial_destino_label);
        holder.tvOrigen.setText(origenLabel + " " + ruta.getOrigenNombre());
        holder.tvDestino.setText(destinoLabel + " " + ruta.getDestinoNombre());
        if (ruta.getTimestamp() != null) {

            Timestamp timestamp = ruta.getTimestamp();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());

            holder.tvFecha.setText(sdf.format(timestamp.toDate()));

        } else {

            holder.tvFecha.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }
    public static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrigen, tvDestino, tvFecha;
        public HistorialViewHolder(@NonNull View itemView) {

            super(itemView);
            tvOrigen = itemView.findViewById(R.id.tvOrigenHistorial);
            tvDestino = itemView.findViewById(R.id.tvDestinoHistorial);
            tvFecha = itemView.findViewById(R.id.tvFechaHistorial);
        }
    }
}