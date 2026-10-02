package com.landlord.android.feature.insights;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.landlord.android.core.network.NetworkModule;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InsightsViewModel extends AndroidViewModel {

    private final InsightsApiService api;
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public InsightsViewModel(@NonNull Application application) {
        super(application);
        api = NetworkModule.getRetrofit(application).create(InsightsApiService.class);
    }

    public LiveData<List<ChatMessage>> messages() {
        return messages;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public void ask(String question) {
        List<ChatMessage> current = new ArrayList<>(messages.getValue());
        current.add(new ChatMessage(question, true, false));
        messages.setValue(current);
        loading.setValue(true);

        api.chat(new ChatRequest(question)).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                loading.postValue(false);
                List<ChatMessage> updated = new ArrayList<>(messages.getValue());
                if (response.isSuccessful() && response.body() != null) {
                    updated.add(new ChatMessage(response.body().answer, false, false));
                } else {
                    updated.add(new ChatMessage("Couldn't get an answer (HTTP " + response.code() + ")", false, true));
                }
                messages.postValue(updated);
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                loading.postValue(false);
                List<ChatMessage> updated = new ArrayList<>(messages.getValue());
                updated.add(new ChatMessage("Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown"), false, true));
                messages.postValue(updated);
            }
        });
    }
}
