package com.landlord.android.feature.payments;

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

public class PaymentHistoryAdapter extends ListAdapter<PaymentEntity, PaymentHistoryAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public interface OnReceiptClick {
        void onClick(PaymentEntity entity);
    }

    private final OnReceiptClick onReceiptClick;

    public PaymentHistoryAdapter(OnReceiptClick onReceiptClick) {
        super(new DiffUtil.ItemCallback<PaymentEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull PaymentEntity a, @NonNull PaymentEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull PaymentEntity a, @NonNull PaymentEntity b) {
                return a.sync.syncState == b.sync.syncState;
            }
        });
        this.onReceiptClick = onReceiptClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        PaymentEntity entity = getItem(position);
        holder.amount.setText(entity.amount + " - " + entity.method);

        boolean synced = entity.sync.syncState == SyncState.SYNCED && entity.sync.serverId != null;
        holder.badge.setVisibility(synced ? View.GONE : View.VISIBLE);
        if (!synced) {
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT ? "Sync issue" : "Pending sync");
        }

        holder.receiptButton.setEnabled(synced);
        holder.receiptButton.setOnClickListener(v -> onReceiptClick.onClick(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView amount;
        final TextView badge;
        final MaterialButton receiptButton;

        VH(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.payment_amount);
            badge = itemView.findViewById(R.id.payment_sync_badge);
            receiptButton = itemView.findViewById(R.id.receipt_button);
        }
    }
}
