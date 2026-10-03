package com.landlord.android.feature.payments;

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
import com.landlord.android.core.ui.ListAnimations;
import java.util.Locale;
import java.util.Objects;

/** Current month's bills table row - mirrors the web generate-bills.component.ts
 *  table: Tenant / Rent / Utilities / Rolled over / Total due / Status. */
public class BillsAdapter extends ListAdapter<BillRow, BillsAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public BillsAdapter() {
        super(new DiffUtil.ItemCallback<BillRow>() {
            @Override
            public boolean areItemsTheSame(@NonNull BillRow a, @NonNull BillRow b) {
                return a.invoiceLocalId.equals(b.invoiceLocalId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull BillRow a, @NonNull BillRow b) {
                return a.balance == b.balance && Objects.equals(a.status, b.status);
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        BillRow bill = getItem(position);

        holder.name.setText(bill.tenantName);
        holder.badge.setText(bill.status);

        int colorRes;
        if ("unpaid".equals(bill.status)) colorRes = R.color.md_error;
        else if ("partial".equals(bill.status)) colorRes = R.color.landlord_gold_accent;
        else colorRes = R.color.md_primary;
        holder.badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), colorRes)));

        holder.breakdown.setText(String.format(Locale.getDefault(),
                "Rent %.2f  •  Utilities %.2f  •  Rolled over %.2f",
                bill.rent, bill.utilitiesTotal, bill.prevUnpaidRolled));

        holder.due.setText("partial".equals(bill.status)
                ? String.format(Locale.getDefault(), "Due: %.2f / %.2f", bill.balance, bill.amount)
                : String.format(Locale.getDefault(), "Due: %.2f", bill.balance));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView badge;
        final TextView breakdown;
        final TextView due;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.bill_tenant_name);
            badge = itemView.findViewById(R.id.bill_status_badge);
            breakdown = itemView.findViewById(R.id.bill_breakdown);
            due = itemView.findViewById(R.id.bill_due);
        }
    }
}
