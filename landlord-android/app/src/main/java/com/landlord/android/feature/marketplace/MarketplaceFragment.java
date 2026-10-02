package com.landlord.android.feature.marketplace;

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
import com.landlord.android.R;

public class MarketplaceFragment extends Fragment {

    private MarketplaceViewModel viewModel;
    private MarketplaceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(MarketplaceViewModel.class);

        RecyclerView list = view.findViewById(R.id.requests_list);
        TextView empty = view.findViewById(R.id.requests_empty);

        adapter = new MarketplaceAdapter(new MarketplaceAdapter.OnDecision() {
            @Override
            public void onApprove(MarketplaceRequestEntity entity) {
                viewModel.approve(entity.localId);
            }

            @Override
            public void onReject(MarketplaceRequestEntity entity) {
                viewModel.reject(entity.localId);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.requests().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
