package com.landlord.android.feature.insights;

/** Local-only, in-memory - the backend is single-turn/stateless
 *  (ChatRequest carries just one question, no history), so this exists
 *  purely to render the conversation on-screen, not sent back to the API. */
public class ChatMessage {
    public final String text;
    public final boolean fromUser;
    public final boolean isError;

    public ChatMessage(String text, boolean fromUser, boolean isError) {
        this.text = text;
        this.fromUser = fromUser;
        this.isError = isError;
    }
}
