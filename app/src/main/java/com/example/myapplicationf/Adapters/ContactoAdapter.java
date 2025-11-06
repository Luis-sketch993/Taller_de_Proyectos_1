package com.example.myapplicationf.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplicationf.Models.ContactoEmergencia;
import com.example.myapplicationf.R;
import java.util.List;

public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder> {

    private List<ContactoEmergencia> contactos;
    private OnContactoDeleteListener deleteListener;

    public interface OnContactoDeleteListener {
        void onDeleteClick(ContactoEmergencia contacto);
    }

    public ContactoAdapter(List<ContactoEmergencia> contactos, OnContactoDeleteListener deleteListener) {
        this.contactos = contactos;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ContactoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contacto_emergencia, parent, false);
        return new ContactoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactoViewHolder holder, int position) {
        ContactoEmergencia contacto = contactos.get(position);
        holder.tvNombre.setText(contacto.getNombre());
        holder.tvTelefono.setText(contacto.getTelefono());
        holder.btnBorrar.setOnClickListener(v -> deleteListener.onDeleteClick(contacto));
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    static class ContactoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvTelefono;
        ImageButton btnBorrar;

        public ContactoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreContacto);
            tvTelefono = itemView.findViewById(R.id.tvTelefonoContacto);
            btnBorrar = itemView.findViewById(R.id.btnBorrarContacto);
        }
    }
}