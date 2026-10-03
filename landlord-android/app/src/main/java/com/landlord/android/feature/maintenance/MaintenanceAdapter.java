package com.landlord.android.feature.maintenance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.landlord.android.R;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.core.ui.ListAnimations;

public class MaintenanceAdapter extends ListAdapter<MaintenanceTicketEntity, MaintenanceAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public interface OnTicketClick {
        void onClick(MaintenanceTicketEntity entity);
    }

    public interface OnPhotoClick {
        void onClick(MaintenanceTicketEntity entity);
    }

    private final OnTicketClick onClick;
    private final OnPhotoClick onPhotoClick;

    public MaintenanceAdapter(OnTicketClick onClick, OnPhotoClick onPhotoClick) {
        super(new DiffUtil.ItemCallback<MaintenanceTicketEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull MaintenanceTicketEntity a, @NonNull MaintenanceTicketEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull MaintenanceTicketEntity a, @NonNull MaintenanceTicketEntity b) {
                return a.status.equals(b.status) && a.sync.syncState == b.sync.syncState
                        && java.util.Objects.equals(a.photoUrl, b.photoUrl)
                        && java.util.Objects.equals(a.localPhotoPath, b.localPhotoPath);
            }
        });
        this.onClick = onClick;
        this.onPhotoClick = onPhotoClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        MaintenanceTicketEntity entity = getItem(position);
        holder.description.setText(entity.description);
        holder.status.setText(entity.status);
        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT
                    ? "Sync issue" : "Pending sync");
        }

        if (entity.photoUrl != null) {
            holder.photoButton.setText("Photo uploaded");
        } else if (entity.localPhotoPath != null) {
            holder.photoButton.setText("Photo pending upload");
        } else {
            holder.photoButton.setText("Add photo");
        }

        holder.itemView.setOnClickListener(v -> onClick.onClick(entity));
        holder.photoButton.setOnClickListener(v -> onPhotoClick.onClick(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView status;
        final TextView badge;
        final MaterialButton photoButton;

        VH(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.ticket_description);
            status = itemView.findViewById(R.id.ticket_status);
            badge = itemView.findViewById(R.id.ticket_sync_badge);
            photoButton = itemView.findViewById(R.id.add_photo_button);
        }
    }
}
