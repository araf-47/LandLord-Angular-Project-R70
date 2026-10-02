package com.landlord.android.feature.messages;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MessagingApiService {

    @GET("api/conversations")
    Call<List<ConversationDto>> listConversations();

    @POST("api/conversations")
    Call<ConversationDto> createConversation(@Body NewConversationRequest request);

    @GET("api/conversations/{id}/messages")
    Call<List<MessageDto>> listMessages(@Path("id") long conversationId);

    @POST("api/conversations/{id}/messages")
    Call<MessageDto> sendMessage(@Path("id") long conversationId, @Body NewMessageRequest request);
}
