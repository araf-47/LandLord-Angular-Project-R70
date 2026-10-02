package com.landlord.android.feature.payments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.landlord.android.R;

public class PaymentHistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PaymentHistoryViewModel viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PaymentHistoryViewModel.class);

        RecyclerView list = view.findViewById(R.id.payment_history_list);
        TextView empty = view.findViewById(R.id.payment_history_empty);

        PaymentHistoryAdapter adapter = new PaymentHistoryAdapter(entity -> {
            Snackbar.make(view, "Downloading receipt...", Snackbar.LENGTH_SHORT).show();
            ReceiptDownloader.downloadAndOpen(requireContext(), entity.sync.serverId, new ReceiptDownloader.Callback() {
                @Override
                public void onSuccess(android.net.Uri uri) {
                    ReceiptDownloader.openPdf(requireContext(), uri);
                }

                @Override
                public void onError(String message) {
                    Snackbar.make(view, "Couldn't get receipt: " + message, Snackbar.LENGTH_LONG).show();
                }
            });
        });

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.payments().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
