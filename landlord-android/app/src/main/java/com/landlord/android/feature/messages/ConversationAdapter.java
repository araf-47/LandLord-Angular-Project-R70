package com.landlord.android.feature.messages;

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

public class ConversationAdapter extends ListAdapter<ConversationEntity, ConversationAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public interface OnClick {
        void onClick(ConversationEntity entity);
    }

    private final OnClick onClick;

    public ConversationAdapter(OnClick onClick) {
        super(new DiffUtil.ItemCallback<ConversationEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ConversationEntity a, @NonNull ConversationEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ConversationEntity a, @NonNull ConversationEntity b) {
                return a.withName.equals(b.withName) && a.sync.syncState == b.sync.syncState;
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        ConversationEntity entity = getItem(position);
        holder.name.setText(entity.withName);
        if (entity.sync.syncState == SyncState.SYNCED) {
            holder.badge.setVisibility(View.GONE);
        } else {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText("Pending sync");
        }
        holder.itemView.setOnClickListener(v -> onClick.onClick(entity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView badge;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.conversation_name);
            badge = itemView.findViewById(R.id.conversation_sync_badge);
        }
    }
}
