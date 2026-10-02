package com.landlord.android.feature.insights;

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
import com.landlord.android.ui.widgets.RequiresInternetBanner;

public class InsightsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InsightsViewModel viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(InsightsViewModel.class);

        RequiresInternetBanner banner = view.findViewById(R.id.insights_offline_banner);
        banner.observe(getViewLifecycleOwner());

        RecyclerView list = view.findViewById(R.id.chat_list);
        android.widget.ProgressBar progress = view.findViewById(R.id.chat_progress);
        TextInputEditText input = view.findViewById(R.id.question_input);
        MaterialButton askButton = view.findViewById(R.id.ask_button);

        ChatAdapter adapter = new ChatAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.messages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (!messages.isEmpty()) list.scrollToPosition(messages.size() - 1);
        });

        viewModel.loading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            askButton.setEnabled(!Boolean.TRUE.equals(loading));
        });

        askButton.setOnClickListener(v -> {
            String question = String.valueOf(input.getText()).trim();
            if (question.isEmpty()) return;
            viewModel.ask(question);
            input.setText("");
        });
    }
}
