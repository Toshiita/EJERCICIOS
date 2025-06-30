package com.example.contactospro;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder> {

    private List<Contacto> contactos;
    private List<Contacto> contactosFiltrados; // Para la búsqueda
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Contacto contacto);
    }

    public ContactoAdapter(List<Contacto> contactos, OnItemClickListener listener) {
        this.contactos = contactos;
        this.contactosFiltrados = new ArrayList<>(contactos); // Inicialmente todos los contactos
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contacto, parent, false);
        return new ContactoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactoViewHolder holder, int position) {
        Contacto contacto = contactosFiltrados.get(position);
        holder.textViewNombre.setText(contacto.getNombre());
        holder.textViewTelefono.setText(contacto.getPais().split(" ")[1] + " " + contacto.getTelefono()); // Mostrar código de país + teléfono

        if (contacto.getImagenUri() != null && !contacto.getImagenUri().isEmpty()) {
            holder.imageViewContacto.setImageURI(Uri.parse(contacto.getImagenUri()));
        } else {
            holder.imageViewContacto.setImageResource(R.drawable.ic_default_profile);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(contacto));
    }

    @Override
    public int getItemCount() {
        return contactosFiltrados.size();
    }

    // Método para filtrar la lista
    public void filter(String text) {
        contactosFiltrados.clear();
        if (text.isEmpty()) {
            contactosFiltrados.addAll(contactos);
        } else {
            text = text.toLowerCase();
            for (Contacto contacto : contactos) {
                if (contacto.getNombre().toLowerCase().contains(text) ||
                        contacto.getTelefono().toLowerCase().contains(text)) {
                    contactosFiltrados.add(contacto);
                }
            }
        }
        notifyDataSetChanged();
    }

    // Método para actualizar la lista de contactos (cuando se actualiza o elimina)
    public void updateList(List<Contacto> newList) {
        this.contactos = newList;
        filter(""); // Reiniciar el filtro para mostrar todos los contactos actualizados
    }

    static class ContactoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewContacto;
        TextView textViewNombre;
        TextView textViewTelefono;

        public ContactoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewContacto = itemView.findViewById(R.id.imageViewContactoLista);
            textViewNombre = itemView.findViewById(R.id.textViewNombreLista);
            textViewTelefono = itemView.findViewById(R.id.textViewTelefonoLista);
        }
    }
}