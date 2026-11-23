package com.bishe;
public interface LlmGateway {
    String generate(String systemPrompt, String userPrompt);
}
