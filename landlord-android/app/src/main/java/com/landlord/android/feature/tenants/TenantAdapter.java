package com.landlord.android.feature.tenants;

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

public class TenantAdapter extends ListAdapter<TenantEntity, TenantAdapter.VH> {

    public interface OnTenantClick {
        void onClick(TenantEntity entity);
    }

    private final OnTenantClick onClick;

    public TenantAdapter(OnTenantClick onClick) {
        super(new DiffUtil.ItemCallback<TenantEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull TenantEntity a, @NonNull TenantEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull TenantEntity a, @NonNull TenantEntity b) {
                return a.name.equals(b.name) && a.sync.syncState == b.sync.syncState;
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tenant, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TenantEntity entity = getItem(position);
        holder.name.setText(entity.name);
        holder.contact.setText(entity.phone + " - " + entity.status);
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
        final TextView contact;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tenant_name);
            contact = itemView.findViewById(R.id.tenant_contact);
            badge = itemView.findViewById(R.id.tenant_sync_badge);
        }
    }
}
