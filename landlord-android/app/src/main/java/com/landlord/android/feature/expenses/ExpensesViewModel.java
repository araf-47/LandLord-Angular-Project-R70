package com.landlord.android.feature.expenses;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.feature.properties.PropertyEntity;
import java.util.List;

public class ExpensesViewModel extends AndroidViewModel {

    private final ExpenseRepository repository;

    public ExpensesViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);
    }

    public LiveData<List<ExpenseEntity>> expenses() {
        return repository.observeAll();
    }

    public LiveData<List<PropertyEntity>> properties() {
        return repository.properties();
    }

    public void createExpense(String propertyLocalId, String category, String description, double amount, String bearer) {
        repository.createExpense(propertyLocalId, category, description, amount, bearer);
    }
}
