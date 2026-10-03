package com.landlord.android.feature.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;
import com.landlord.android.core.ui.ListAnimations;

public class NotificationAdapter extends ListAdapter<NotificationEntity, NotificationAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public interface OnClick {
        void onClick(NotificationEntity entity);
    }

    private final OnClick onClick;

    public NotificationAdapter(OnClick onClick) {
        super(new DiffUtil.ItemCallback<NotificationEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull NotificationEntity a, @NonNull NotificationEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull NotificationEntity a, @NonNull NotificationEntity b) {
                return a.read == b.read;
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        NotificationEntity entity = getItem(position);
        holder.title.setText(entity.title);
        holder.body.setText(entity.body);
        holder.title.setTypeface(null, entity.read ? Typeface.NORMAL : Typeface.BOLD);
        holder.itemView.setOnClickListener(v -> onClick.onClick(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView body;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            body = itemView.findViewById(R.id.notification_body);
        }
    }
}
