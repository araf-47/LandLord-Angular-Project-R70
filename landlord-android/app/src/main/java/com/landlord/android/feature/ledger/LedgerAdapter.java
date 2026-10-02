package com.landlord.android.feature.ledger;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;

public class LedgerAdapter extends ListAdapter<LedgerEntry, LedgerAdapter.VH> {

    public LedgerAdapter() {
        super(new DiffUtil.ItemCallback<LedgerEntry>() {
            @Override
            public boolean areItemsTheSame(@NonNull LedgerEntry a, @NonNull LedgerEntry b) {
                return a.date == b.date && a.description.equals(b.description);
            }

            @Override
            public boolean areContentsTheSame(@NonNull LedgerEntry a, @NonNull LedgerEntry b) {
                return a.amount == b.amount && java.util.Objects.equals(a.syncBadge, b.syncBadge);
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ledger_entry, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        LedgerEntry entry = getItem(position);
        holder.description.setText(entry.description);
        holder.type.setText(entry.type + (entry.syncBadge != null ? " - " + entry.syncBadge : ""));
        holder.amount.setText((entry.amount >= 0 ? "+" : "") + entry.amount);
        holder.amount.setTextColor(entry.amount >= 0 ? Color.parseColor("#2E6F40") : Color.parseColor("#B3261E"));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView type;
        final TextView amount;

        VH(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.ledger_description);
            type = itemView.findViewById(R.id.ledger_type);
            amount = itemView.findViewById(R.id.ledger_amount);
        }
    }
}
