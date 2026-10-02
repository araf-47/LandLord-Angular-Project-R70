package com.landlord.android.feature.tenants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.common.Result;

public class TenantDetailFragment extends Fragment {

    private TenantDetailViewModel viewModel;
    private String tenantLocalId;
    private Long tenantServerId;
    private String currentTerms = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tenant_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tenantLocalId = requireArguments().getString("tenantLocalId");

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TenantDetailViewModel.class);

        TextView nameView = view.findViewById(R.id.tenant_name);
        TextView contactView = view.findViewById(R.id.tenant_contact);
        TextView statusView = view.findViewById(R.id.tenant_status);
        ProgressBar agreementProgress = view.findViewById(R.id.agreement_progress);
        TextView agreementStatus = view.findViewById(R.id.agreement_status);
        MaterialCardView agreementCard = view.findViewById(R.id.agreement_card);
        TextView termsView = view.findViewById(R.id.agreement_terms);
        TextView depositView = view.findViewById(R.id.agreement_deposit);
        TextView startDateView = view.findViewById(R.id.agreement_start_date);

        viewModel.tenant(tenantLocalId).observe(getViewLifecycleOwner(), tenant -> {
            if (tenant == null) return;

            nameView.setText(tenant.name);
            contactView.setText(tenant.phone + (tenant.email != null && !tenant.email.isEmpty() ? " - " + tenant.email : ""));
            statusView.setText(tenant.status + " - NID: " + tenant.nationalId);

            if (tenant.sync.serverId == null) {
                agreementProgress.setVisibility(View.GONE);
                agreementStatus.setText("Waiting for this tenant to sync before the agreement can be loaded");
                return;
            }

            if (tenantServerId == null) {
                tenantServerId = tenant.sync.serverId;
                loadAgreement(agreementProgress, agreementStatus, agreementCard, termsView, depositView, startDateView);
            }
        });

        view.findViewById(R.id.edit_terms_button).setOnClickListener(v -> showEditTermsDialog(view));
    }

    private void loadAgreement(ProgressBar progress, TextView status, MaterialCardView card,
                                TextView terms, TextView deposit, TextView startDate) {
        progress.setVisibility(View.VISIBLE);
        status.setText("");
        card.setVisibility(View.GONE);

        viewModel.getAgreement(tenantServerId).observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                progress.setVisibility(View.VISIBLE);
            } else if (result instanceof Result.Success) {
                progress.setVisibility(View.GONE);
                RentalAgreementDto agreement = ((Result.Success<RentalAgreementDto>) result).data;
                currentTerms = agreement.terms != null ? agreement.terms : "";
                terms.setText(currentTerms);
                deposit.setText("Deposit: " + agreement.deposit);
                startDate.setText("Since: " + agreement.startDate);
                card.setVisibility(View.VISIBLE);
            } else if (result instanceof Result.Error) {
                progress.setVisibility(View.GONE);
                status.setText(((Result.Error<?>) result).message);
            }
        });
    }

    private void showEditTermsDialog(View rootView) {
        if (tenantServerId == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_terms, null);
        TextInputEditText termsInput = dialogView.findViewById(R.id.input_terms);
        termsInput.setText(currentTerms);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit agreement terms")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTerms = String.valueOf(termsInput.getText()).trim();
                    viewModel.updateTerms(tenantServerId, newTerms).observe(getViewLifecycleOwner(), result -> {
                        if (result instanceof Result.Success) {
                            com.google.android.material.snackbar.Snackbar.make(rootView, "Terms updated",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                            currentTerms = newTerms;
                            TextView termsView = rootView.findViewById(R.id.agreement_terms);
                            termsView.setText(newTerms);
                        } else if (result instanceof Result.Error) {
                            com.google.android.material.snackbar.Snackbar.make(rootView,
                                    "Failed: " + ((Result.Error<?>) result).message,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
