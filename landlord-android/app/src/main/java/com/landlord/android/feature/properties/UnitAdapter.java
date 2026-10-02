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

public class UnitAdapter extends ListAdapter<UnitEntity, UnitAdapter.VH> {

    public UnitAdapter() {
        super(new DiffUtil.ItemCallback<UnitEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull UnitEntity a, @NonNull UnitEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull UnitEntity a, @NonNull UnitEntity b) {
                return a.unitNumber.equals(b.unitNumber) && a.sync.syncState == b.sync.syncState;
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unit, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UnitEntity entity = getItem(position);
        holder.number.setText(entity.unitNumber);
        holder.rentStatus.setText("Rent: " + entity.rent + " - " + entity.status);
        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT
                    ? "Sync issue" : "Pending sync");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView rentStatus;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.unit_number);
            rentStatus = itemView.findViewById(R.id.unit_rent_status);
            badge = itemView.findViewById(R.id.unit_sync_badge);
        }
    }
}
