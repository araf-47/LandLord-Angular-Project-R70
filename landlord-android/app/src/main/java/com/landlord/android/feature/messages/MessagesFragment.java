package com.landlord.android.feature.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    private MessagesViewModel viewModel;
    private ConversationAdapter adapter;
    private List<TenantEntity> availableTenants = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(MessagesViewModel.class);

        RecyclerView list = view.findViewById(R.id.conversations_list);
        TextView empty = view.findViewById(R.id.conversations_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_conversation_fab);

        adapter = new ConversationAdapter(entity -> {
            Bundle args = new Bundle();
            args.putString("conversationLocalId", entity.localId);
            args.putString("withName", entity.withName);
            Navigation.findNavController(view).navigate(R.id.action_messages_to_thread, args);
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.conversations().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.tenants().observe(getViewLifecycleOwner(), tenants -> availableTenants = tenants);

        fab.setOnClickListener(v -> showNewConversationDialog());
    }

    private void showNewConversationDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_conversation, null);

        Spinner tenantSpinner = dialogView.findViewById(R.id.input_tenant);
        TextInputEditText withNameInput = dialogView.findViewById(R.id.input_with_name);

        List<String> labels = new ArrayList<>();
        labels.add("(none)");
        for (TenantEntity t : availableTenants) labels.add(t.name);
        tenantSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New conversation")
                .setView(dialogView)
                .setPositiveButton("Start", (dialog, which) -> {
                    String withName = String.valueOf(withNameInput.getText()).trim();
                    if (withName.isEmpty()) return;

                    int selected = tenantSpinner.getSelectedItemPosition();
                    String tenantLocalId = (selected > 0 && selected - 1 < availableTenants.size())
                            ? availableTenants.get(selected - 1).localId : null;

                    viewModel.createConversation(tenantLocalId, withName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
