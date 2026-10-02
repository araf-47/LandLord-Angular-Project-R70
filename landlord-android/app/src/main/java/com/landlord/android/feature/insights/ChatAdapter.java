package com.landlord.android.feature.insights;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.landlord.android.R;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> items = new ArrayList<>();

    public void submitList(List<ChatMessage> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
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
        ChatMessage message = items.get(position);
        holder.text.setText(message.text);
        holder.text.setTextColor(message.isError ? 0xFFB3261E : 0xFF000000);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.text.getLayoutParams();
        params.gravity = message.fromUser ? Gravity.END : Gravity.START;
        holder.text.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.chat_message_text);
        }
    }
}
