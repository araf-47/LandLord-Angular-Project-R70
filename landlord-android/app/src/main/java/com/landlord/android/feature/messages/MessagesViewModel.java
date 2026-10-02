package com.landlord.android.feature.messages;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.List;

public class MessagesViewModel extends AndroidViewModel {

    private final MessagingRepository repository;

    public MessagesViewModel(@NonNull Application application) {
        super(application);
        repository = new MessagingRepository(application);
    }

    public LiveData<List<ConversationEntity>> conversations() {
        return repository.observeConversations();
    }

    public LiveData<List<TenantEntity>> tenants() {
        return repository.tenants();
    }

    public LiveData<List<MessageEntity>> messages(String conversationLocalId) {
        return repository.observeMessages(conversationLocalId);
    }

    public void createConversation(String tenantLocalId, String withName) {
        repository.createConversation(tenantLocalId, withName);
    }

    public void sendMessage(String conversationLocalId, String text) {
        repository.sendMessage(conversationLocalId, text);
    }
}
