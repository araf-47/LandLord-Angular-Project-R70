package com.landlord.android.feature.payments;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class PaymentHistoryViewModel extends AndroidViewModel {

    private final PaymentRepository repository;

    public PaymentHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new PaymentRepository(application);
    }

    public LiveData<List<PaymentEntity>> payments() {
        return repository.observeAll();
    }
}
