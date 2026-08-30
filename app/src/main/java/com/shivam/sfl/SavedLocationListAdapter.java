package com.shivam.sfl;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SavedLocationListAdapter extends RecyclerView.Adapter<SavedLocationListAdapter.ViewHolder> implements Filterable {
    private final List<SavedLocationEntity> _mSavedLocationEntities;
    private final List<SavedLocationEntity> mSavedLocationEntities;
    private final Set<SavedLocationEntity> selectedItems = new LinkedHashSet<>();
    private DatabaseHandler mDatabaseHandler;
    private Runnable onDataChangedListener;
    private SelectionListener selectionListener;

    public interface SelectionListener {
        void onSelectionChanged(int count);
    }

    public SavedLocationListAdapter(List<SavedLocationEntity> mSavedLocationEntities) {
        this.mSavedLocationEntities = mSavedLocationEntities;
        this._mSavedLocationEntities = new ArrayList<>(mSavedLocationEntities);
    }

    public void setOnDataChangedListener(Runnable listener) {
        this.onDataChangedListener = listener;
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public List<SavedLocationEntity> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public void removeSelectedFromList() {
        for (SavedLocationEntity entity : selectedItems) {
            int pos = mSavedLocationEntities.indexOf(entity);
            if (pos != -1) mSavedLocationEntities.remove(pos);
            _mSavedLocationEntities.remove(entity);
        }
        selectedItems.clear();
        notifyDataSetChanged();
        if (onDataChangedListener != null) onDataChangedListener.run();
    }

    private void toggleSelection(SavedLocationEntity entity) {
        if (!selectedItems.remove(entity)) selectedItems.add(entity);
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionChanged(selectedItems.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_saved_location_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final SavedLocationEntity entity = this.mSavedLocationEntities.get(position);
        holder.name.setText(entity.getName());
        
        String displayCoords = String.format(java.util.Locale.US, "%.5f, %.5f", entity.getLat(), entity.getLongt());
        if (entity.getPlusCode() != null && !entity.getPlusCode().isEmpty()) {
            displayCoords += " (" + entity.getPlusCode() + ")";
        }
        holder.co_ordinate.setText(displayCoords);

        holder.address.setText(entity.getAddress());
        holder.type.setText(entity.getType());
        holder.time.setText(entity.getTimeStamp());

        boolean inSelectionMode = !selectedItems.isEmpty();
        if (entity.getContactName() != null && !entity.getContactName().isEmpty()) {
            holder.contactName.setText(entity.getContactName());
            holder.contactRow.setVisibility(View.VISIBLE);
            holder.contactRow.setClickable(!inSelectionMode);
            holder.contactRow.setOnClickListener(inSelectionMode ? null : v -> {
                try {
                    v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entity.getContactId())));
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Couldn't open contact", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.contactRow.setVisibility(View.GONE);
        }

        boolean selected = selectedItems.contains(entity);
        holder.cardView.setCheckable(true);
        holder.cardView.setChecked(selected);

        int actionIconVisibility = selectedItems.isEmpty() ? View.VISIBLE : View.GONE;
        holder.map.setVisibility(actionIconVisibility);
        holder.share.setVisibility(actionIconVisibility);
        holder.edit.setVisibility(actionIconVisibility);
        holder.delete.setVisibility(actionIconVisibility);

        holder.cardView.setOnLongClickListener(v -> {
            toggleSelection(entity);
            return true;
        });
        holder.cardView.setOnClickListener(v -> {
            if (!selectedItems.isEmpty()) toggleSelection(entity);
        });

        if (entity.getDistanceMeters() >= 0) {
            float meters = entity.getDistanceMeters();
            holder.distance.setText(meters < 1000
                    ? String.format(java.util.Locale.US, "%.0f m away", meters)
                    : String.format(java.util.Locale.US, "%.1f km away", meters / 1000f));
            holder.distance.setVisibility(View.VISIBLE);
        } else {
            holder.distance.setVisibility(View.GONE);
        }

        holder.map.setOnClickListener(v -> {
            String uri = "http://maps.google.com/maps?q=loc:" + entity.getLat() + "," + entity.getLongt() + " (" + entity.getName() + ")";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            v.getContext().startActivity(mapIntent);
        });

        holder.share.setOnClickListener(v -> {
            String shareBody = "Name: " + entity.getName() + "\nCoordinates: http://maps.google.com/maps?q=" + entity.getLat() + "," + entity.getLongt() + "\nAddress: " + entity.getAddress() + "\nType: " + entity.getType();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Location Sharing");
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
            v.getContext().startActivity(Intent.createChooser(intent, "Share via"));
        });

        holder.edit.setOnClickListener(v -> {
            Intent mIntent = new Intent(v.getContext(), CompleteSaveLocation.class);
            mIntent.putExtra("name", entity.getName());
            mIntent.putExtra("co-ordinates", entity.getLat() + "," + entity.getLongt());
            mIntent.putExtra("address", entity.getAddress());
            mIntent.putExtra("type", entity.getType());
            mIntent.putExtra("id", entity.getID());
            mIntent.putExtra("contact_id", entity.getContactId());
            mIntent.putExtra("contact_name", entity.getContactName());
            v.getContext().startActivity(mIntent);
        });

        holder.delete.setOnClickListener(v -> {
            int removedPos = mSavedLocationEntities.indexOf(entity);
            if (removedPos == -1) return;

            mSavedLocationEntities.remove(removedPos);
            _mSavedLocationEntities.remove(entity);
            notifyItemRemoved(removedPos);
            if (onDataChangedListener != null) onDataChangedListener.run();

            Snackbar.make(v, "Location deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", undoClick -> {
                    mSavedLocationEntities.add(removedPos, entity);
                    _mSavedLocationEntities.add(removedPos, entity);
                    notifyItemInserted(removedPos);
                    if (onDataChangedListener != null) onDataChangedListener.run();
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event == DISMISS_EVENT_ACTION) return; // undone, nothing to delete
                        if (mDatabaseHandler == null) mDatabaseHandler = new DatabaseHandler(v.getContext());
                        mDatabaseHandler.deleteLocation(entity.getID());
                        QuickSetLocations.clearIfMatches(v.getContext(), entity.getID());
                    }
                })
                .show();
        });
    }

    @Override
    public int getItemCount() {
        return this.mSavedLocationEntities.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<SavedLocationEntity> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(_mSavedLocationEntities);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (SavedLocationEntity item : _mSavedLocationEntities) {
                        if (item.getType().toLowerCase().contains(pattern)
                                || item.getName().toLowerCase().contains(pattern)
                                || (item.getAddress() != null && item.getAddress().toLowerCase().contains(pattern))) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                final List<SavedLocationEntity> newList = (List<SavedLocationEntity>) results.values;
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override public int getOldListSize() { return mSavedLocationEntities.size(); }
                    @Override public int getNewListSize() { return newList.size(); }
                    @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                        return mSavedLocationEntities.get(oldPos).getID() == newList.get(newPos).getID();
                    }
                    @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                        return mSavedLocationEntities.get(oldPos).toString().equals(newList.get(newPos).toString());
                    }
                });
                mSavedLocationEntities.clear();
                mSavedLocationEntities.addAll(newList);
                diffResult.dispatchUpdatesTo(SavedLocationListAdapter.this);
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView address, co_ordinate, distance, name, time, type, contactName;
        final ImageButton delete, edit, map, share;
        final View contactRow;
        final MaterialCardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            name = itemView.findViewById(R.id.name);
            co_ordinate = itemView.findViewById(R.id.co_ordinates);
            address = itemView.findViewById(R.id.address);
            distance = itemView.findViewById(R.id.distance);
            type = itemView.findViewById(R.id.type);
            time = itemView.findViewById(R.id.time);
            map = itemView.findViewById(R.id.map);
            share = itemView.findViewById(R.id.share);
            edit = itemView.findViewById(R.id.edit);
            delete = itemView.findViewById(R.id.delete);
            contactRow = itemView.findViewById(R.id.contact_row);
            contactName = itemView.findViewById(R.id.contact_name);
        }
    }
}
