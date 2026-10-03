package com.landlord.android.feature.insights;

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
import com.google.android.material.R.attr;
import com.google.android.material.color.MaterialColors;
import com.landlord.android.R;
import com.landlord.android.core.ui.ListAnimations;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.VH> {

    private final ListAnimations listAnimations = new ListAnimations();

    public ChatAdapter() {
        super(new DiffUtil.ItemCallback<ChatMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                return a == b;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                return a.text.equals(b.text) && a.fromUser == b.fromUser && a.isError == b.isError;
            }
        });
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_bubble, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        listAnimations.animate(holder.itemView, position);
        ChatMessage message = getItem(position);
        holder.text.setText(message.text);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.text.getLayoutParams();
        params.gravity = message.fromUser ? Gravity.END : Gravity.START;
        holder.text.setLayoutParams(params);

        holder.text.setBackgroundResource(message.fromUser
                ? R.drawable.bg_message_bubble_mine
                : R.drawable.bg_message_bubble_theirs);

        int textColorAttr = message.isError
                ? attr.colorError
                : (message.fromUser ? attr.colorOnPrimaryContainer : attr.colorOnSurface);
        holder.text.setTextColor(MaterialColors.getColor(holder.text, textColorAttr));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.chat_message_text);
        }
    }
}
