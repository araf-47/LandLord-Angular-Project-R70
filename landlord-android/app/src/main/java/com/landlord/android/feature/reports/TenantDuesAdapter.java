package com.landlord.android.feature.reports;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;
import java.util.Locale;
import java.util.Objects;

/** Row renderer for the Tenant Dues report - replaces the raw JSON dump with a
 *  plain tenant/amount/status list, same visual language as the Payments tab's
 *  Monthly Bills rows. */
public class TenantDuesAdapter extends ListAdapter<TenantDueRow, TenantDuesAdapter.VH> {

    public TenantDuesAdapter() {
        super(new DiffUtil.ItemCallback<TenantDueRow>() {
            @Override
            public boolean areItemsTheSame(@NonNull TenantDueRow a, @NonNull TenantDueRow b) {
                return a.tenant.equals(b.tenant);
            }

            @Override
            public boolean areContentsTheSame(@NonNull TenantDueRow a, @NonNull TenantDueRow b) {
                return a.totalDue == b.totalDue && Objects.equals(a.status, b.status);
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_tenant_due, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TenantDueRow row = getItem(position);
        holder.name.setText(row.tenant);
        holder.amount.setText(String.format(Locale.getDefault(), "%.2f", row.totalDue));
        holder.badge.setText(row.status);

        int colorRes = "unpaid".equals(row.status) ? R.color.md_error : R.color.landlord_gold_accent;
        holder.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), colorRes)));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView amount;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.row_tenant_name);
            amount = itemView.findViewById(R.id.row_due_amount);
            badge = itemView.findViewById(R.id.row_status_badge);
        }
    }
}
