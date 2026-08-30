package com.shivam.sfl;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CollectionListAdapter extends RecyclerView.Adapter<CollectionListAdapter.ViewHolder> {
    public interface Listener {
        void onCollectionClick(CollectionEntity collection);
        void onCollectionDelete(CollectionEntity collection);
        void onCollectionShare(CollectionEntity collection);
    }

    private final List<CollectionEntity> collections;
    private final Listener listener;

    public CollectionListAdapter(List<CollectionEntity> collections, Listener listener) {
        this.collections = collections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_collections_item, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CollectionEntity collection = collections.get(position);
        holder.name.setText(collection.getName());
        holder.count.setText(collection.getLocationCount() == 1 ? "1 location" : collection.getLocationCount() + " locations");
        holder.itemView.setOnClickListener(v -> listener.onCollectionClick(collection));
        holder.delete.setOnClickListener(v -> listener.onCollectionDelete(collection));
        holder.share.setOnClickListener(v -> listener.onCollectionShare(collection));
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, count;
        final ImageButton delete, share;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.collection_name);
            count = itemView.findViewById(R.id.collection_count);
            delete = itemView.findViewById(R.id.collection_delete);
            share = itemView.findViewById(R.id.collection_share);
        }
    }
}
