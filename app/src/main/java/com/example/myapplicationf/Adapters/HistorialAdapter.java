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

    private final List<HistorialRuta> historialList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistorialRuta historialRuta);
    }

    public HistorialAdapter(List<HistorialRuta> historialList, OnItemClickListener listener) {
        this.historialList = historialList;
        this.listener = listener;
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
        holder.bind(ruta, listener);
    }

    @Override
    public int getItemCount() {
        return historialList.size();
    }

    public void updateData(List<HistorialRuta> nuevasRutas) {
        this.historialList.clear();
        this.historialList.addAll(nuevasRutas);
        notifyDataSetChanged();
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrigen, tvDestino, tvFecha;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            // --- 🔹 CAMBIO CLAVE: Corregir los IDs para que coincidan con el XML ---
            tvOrigen = itemView.findViewById(R.id.tvOrigenHistorial);
            tvDestino = itemView.findViewById(R.id.tvDestinoHistorial);
            tvFecha = itemView.findViewById(R.id.tvFechaHistorial);
        }

        public void bind(final HistorialRuta ruta, final OnItemClickListener listener) {
            String origenLabel = itemView.getContext().getString(R.string.historial_origen_label);
            String destinoLabel = itemView.getContext().getString(R.string.historial_destino_label);

            tvOrigen.setText(origenLabel + " " + ruta.getOrigenNombre());
            tvDestino.setText(destinoLabel + " " + ruta.getDestinoNombre());

            if (ruta.getTimestamp() != null) {
                Timestamp timestamp = ruta.getTimestamp();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                tvFecha.setText(sdf.format(timestamp.toDate()));
            } else {
                tvFecha.setText("");
            }

            itemView.setOnClickListener(v -> listener.onItemClick(ruta));
        }
    }
}

