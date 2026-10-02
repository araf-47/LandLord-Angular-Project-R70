package com.landlord.android.feature.messages;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;

public class MessageBubbleAdapter extends ListAdapter<MessageEntity, MessageBubbleAdapter.VH> {

    public MessageBubbleAdapter() {
        super(new DiffUtil.ItemCallback<MessageEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull MessageEntity a, @NonNull MessageEntity b) {
                return a.localId.equals(b.localId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull MessageEntity a, @NonNull MessageEntity b) {
                return a.text.equals(b.text) && a.sync.syncState == b.sync.syncState;
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_bubble, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MessageEntity entity = getItem(position);
        boolean fromLandlord = "landlord".equals(entity.senderRole);
        holder.text.setText(entity.text + (entity.sync.syncState.name().startsWith("PENDING") ? " (sending...)" : ""));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.text.getLayoutParams();
        params.gravity = fromLandlord ? Gravity.END : Gravity.START;
        holder.text.setLayoutParams(params);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.message_text);
        }
    }
}
