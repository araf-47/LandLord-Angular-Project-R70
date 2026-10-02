package com.landlord.android.feature.payments;

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

public class InvoiceAdapter extends ListAdapter<InvoiceEntity, InvoiceAdapter.VH> {

    public interface OnInvoiceClick {
        void onClick(InvoiceEntity entity);
    }

    private final OnInvoiceClick onClick;

    public InvoiceAdapter(OnInvoiceClick onClick) {
        super(new DiffUtil.ItemCallback<InvoiceEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull InvoiceEntity a, @NonNull InvoiceEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull InvoiceEntity a, @NonNull InvoiceEntity b) {
                return a.sync.syncState == b.sync.syncState
                        && java.util.Objects.equals(a.balance, b.balance)
                        && java.util.Objects.equals(a.status, b.status);
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        InvoiceEntity entity = getItem(position);
        holder.period.setText("Invoice" + (entity.period != null ? " - " + entity.period : " (generating...)"));
        String balanceText = entity.balance != null ? String.valueOf(entity.balance) : "?";
        holder.amount.setText("Balance: " + balanceText + " - " + entity.status);
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
        final TextView period;
        final TextView amount;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            period = itemView.findViewById(R.id.invoice_period);
            amount = itemView.findViewById(R.id.invoice_amount);
            badge = itemView.findViewById(R.id.invoice_sync_badge);
        }
    }
}
