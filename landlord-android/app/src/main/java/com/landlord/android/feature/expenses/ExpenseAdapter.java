package com.landlord.android.feature.expenses;

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
import com.landlord.android.core.ui.ListAnimations;

public class ExpenseAdapter extends ListAdapter<ExpenseEntity, ExpenseAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public ExpenseAdapter() {
        super(new DiffUtil.ItemCallback<ExpenseEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ExpenseEntity a, @NonNull ExpenseEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ExpenseEntity a, @NonNull ExpenseEntity b) {
                return a.category.equals(b.category) && a.sync.syncState == b.sync.syncState;
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        ExpenseEntity entity = getItem(position);
        holder.category.setText(entity.category);
        holder.amount.setText(entity.amount + " - " + entity.bearer);
        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(entity.sync.syncState == SyncState.CONFLICT
                    ? "Sync issue" : "Pending sync");
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView category;
        final TextView amount;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.expense_category);
            amount = itemView.findViewById(R.id.expense_amount);
            badge = itemView.findViewById(R.id.expense_sync_badge);
        }
    }
}
