package com.landlord.android.feature.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;

public class MessageThreadFragment extends Fragment {

    private MessagesViewModel viewModel;
    private String conversationLocalId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_thread, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        conversationLocalId = requireArguments().getString("conversationLocalId");
        String withName = requireArguments().getString("withName");
        if (getActivity() != null && withName != null) {
            requireActivity().setTitle(withName);
        }

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(MessagesViewModel.class);

        RecyclerView list = view.findViewById(R.id.messages_list);
        TextInputEditText input = view.findViewById(R.id.message_input);
        MaterialButton send = view.findViewById(R.id.send_button);

        MessageBubbleAdapter adapter = new MessageBubbleAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.messages(conversationLocalId).observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (messages != null && !messages.isEmpty()) list.scrollToPosition(messages.size() - 1);
        });

        send.setOnClickListener(v -> {
            String text = String.valueOf(input.getText()).trim();
            if (text.isEmpty()) return;
            viewModel.sendMessage(conversationLocalId, text);
            input.setText("");
        });
    }
}
