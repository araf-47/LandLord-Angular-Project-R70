package com.landlord.android.feature.maintenance;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.feature.properties.UnitEntity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceFragment extends Fragment {

    private MaintenanceViewModel viewModel;
    private MaintenanceAdapter adapter;
    private List<UnitEntity> availableUnits = new ArrayList<>();
    private String pendingPhotoTicketLocalId;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maintenance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(MaintenanceViewModel.class);

        RecyclerView list = view.findViewById(R.id.tickets_list);
        TextView empty = view.findViewById(R.id.tickets_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_ticket_fab);

        adapter = new MaintenanceAdapter(this::showResolveDialogIfPending, ticket -> {
            pendingPhotoTicketLocalId = ticket.localId;
            pickImage.launch("image/*");
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.tickets().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.units().observe(getViewLifecycleOwner(), units -> availableUnits = units);

        fab.setOnClickListener(v -> showAddDialog());
    }

    private void onImagePicked(Uri uri) {
        if (uri == null || pendingPhotoTicketLocalId == null) return;
        String ticketLocalId = pendingPhotoTicketLocalId;

        Context appContext = requireContext().getApplicationContext();
        try {
            File dir = new File(appContext.getFilesDir(), "maintenance_photos");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, ticketLocalId + ".jpg");

            try (InputStream in = appContext.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(file)) {
                if (in == null) throw new IOException("could not open picked image");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            viewModel.attachPhoto(ticketLocalId, file.getAbsolutePath());
            Snackbar.make(requireView(), "Photo queued for upload", Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Snackbar.make(requireView(), "Couldn't read photo: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void showResolveDialogIfPending(MaintenanceTicketEntity ticket) {
        if (!"pending".equals(ticket.status)) {
            Snackbar.make(requireView(), "Ticket already " + ticket.status, Snackbar.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_resolve_ticket, null);
        TextInputEditText costInput = dialogView.findViewById(R.id.input_cost);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Resolve ticket")
                .setView(dialogView)
                .setPositiveButton("Mark resolved", (dialog, which) -> {
                    double cost;
                    try {
                        cost = Double.parseDouble(String.valueOf(costInput.getText()).trim());
                    } catch (NumberFormatException e) {
                        cost = 0;
                    }
                    viewModel.resolveTicket(ticket.localId, cost);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDialog() {
        if (availableUnits.isEmpty()) {
            Snackbar.make(requireView(), "Add a property and unit first", Snackbar.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_ticket, null);

        Spinner unitSpinner = dialogView.findViewById(R.id.input_unit);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.input_description);

        List<String> labels = new ArrayList<>();
        for (UnitEntity u : availableUnits) labels.add(u.unitNumber);
        unitSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Report maintenance issue")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selected = unitSpinner.getSelectedItemPosition();
                    if (selected < 0 || selected >= availableUnits.size()) return;

                    String description = String.valueOf(descriptionInput.getText()).trim();
                    if (description.isEmpty()) return;

                    viewModel.createTicket(availableUnits.get(selected).localId, description);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
