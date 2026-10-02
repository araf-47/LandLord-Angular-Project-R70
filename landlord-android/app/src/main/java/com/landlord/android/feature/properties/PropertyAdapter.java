package com.landlord.android.feature.properties;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;
import com.landlord.android.core.sync.SyncState;

public class PropertyAdapter extends ListAdapter<PropertyEntity, PropertyAdapter.VH> {

    public interface OnPropertyClick {
        void onClick(PropertyEntity entity);
    }

    private final OnPropertyClick onClick;

    public PropertyAdapter(OnPropertyClick onClick) {
        super(new DiffUtil.ItemCallback<PropertyEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull PropertyEntity a, @NonNull PropertyEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull PropertyEntity a, @NonNull PropertyEntity b) {
                return a.name.equals(b.name) && a.sync.syncState == b.sync.syncState;
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_property, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PropertyEntity entity = getItem(position);
        holder.name.setText(entity.name);
        holder.address.setText(entity.address);
        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT
                    ? "Sync issue" : "Pending sync");
        }
        holder.itemView.setOnClickListener(v -> onClick.onClick(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView address;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.property_name);
            address = itemView.findViewById(R.id.property_address);
            badge = itemView.findViewById(R.id.property_sync_badge);
        }
    }
}
