package com.landlord.backend.insights;

import org.springframework.stereotype.Service;

@Service
public class InsightsService {

    private final InsightsContextBuilder contextBuilder;
    private final GeminiClient geminiClient;

    public InsightsService(InsightsContextBuilder contextBuilder, GeminiClient geminiClient) {
        this.contextBuilder = contextBuilder;
        this.geminiClient = geminiClient;
    }

    public String ask(String question) {
        String context = contextBuilder.build();
        String prompt = """
            You are a helpful assistant for a landlord using a rental property management app.
            Answer the landlord's question using ONLY the portfolio data below - don't invent
            figures that aren't there. If the data doesn't cover the question, say so plainly.
            Keep the answer concise and practical.

            %s

            Landlord's question: %s
            """.formatted(context, question);
        return geminiClient.generate(prompt);
    }
}
