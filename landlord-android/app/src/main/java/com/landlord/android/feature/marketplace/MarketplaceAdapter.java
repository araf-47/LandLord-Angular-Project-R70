package com.landlord.android.feature.marketplace;

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

public class MarketplaceAdapter extends ListAdapter<MarketplaceRequestEntity, MarketplaceAdapter.VH> {

    public interface OnDecision {
        void onApprove(MarketplaceRequestEntity entity);
        void onReject(MarketplaceRequestEntity entity);
    }

    private final OnDecision onDecision;

    public MarketplaceAdapter(OnDecision onDecision) {
        super(new DiffUtil.ItemCallback<MarketplaceRequestEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull MarketplaceRequestEntity a, @NonNull MarketplaceRequestEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull MarketplaceRequestEntity a, @NonNull MarketplaceRequestEntity b) {
                return a.status.equals(b.status) && a.sync.syncState == b.sync.syncState;
            }
        });
        this.onDecision = onDecision;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketplace_request, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MarketplaceRequestEntity entity = getItem(position);
        holder.applicant.setText(entity.applicantName);
        holder.status.setText(entity.status);

        boolean decided = !"pending".equals(entity.status);
        holder.actions.setVisibility(decided ? View.GONE : View.VISIBLE);

        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT
                    ? "Sync issue" : "Pending sync");
        }

        holder.approve.setOnClickListener(v -> onDecision.onApprove(entity));
        holder.reject.setOnClickListener(v -> onDecision.onReject(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView applicant;
        final TextView status;
        final TextView badge;
        final View actions;
        final MaterialButton approve;
        final MaterialButton reject;

        VH(@NonNull View itemView) {
            super(itemView);
            applicant = itemView.findViewById(R.id.request_applicant);
            status = itemView.findViewById(R.id.request_status);
            badge = itemView.findViewById(R.id.request_sync_badge);
            actions = itemView.findViewById(R.id.request_actions);
            approve = itemView.findViewById(R.id.approve_button);
            reject = itemView.findViewById(R.id.reject_button);
        }
    }
}
