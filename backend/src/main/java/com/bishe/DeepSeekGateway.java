package com.bishe;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class DeepSeekGateway implements LlmGateway {
    private static final String MODEL="deepseek-flash";
    private final RestClient client;
    private final String apiKey;
    @Autowired
    DeepSeekGateway(@Value("${DEEPSEEK_API_KEY:}") String apiKey,RestClient.Builder builder){
        this.apiKey=apiKey;
        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(10000); factory.setReadTimeout(60000);
        this.client=builder.baseUrl("https://api.deepseek.com").requestFactory(factory).build();
    }
    DeepSeekGateway(String apiKey, RestClient client) { this.apiKey=apiKey; this.client=client; }
    @Override public String generate(String systemPrompt,String userPrompt){
        if(apiKey==null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"尚未配置 DeepSeek API 密钥，请联系管理员配置。 ");
        try {
            JsonNode result=client.post().uri("/chat/completions").header("Authorization","Bearer "+apiKey)
                .body(Map.of("model",MODEL,"temperature",0.2,"max_tokens",1500,"thinking",Map.of("type","disabled"),"messages",List.of(Map.of("role","system","content",systemPrompt),Map.of("role","user","content",userPrompt))))
                .retrieve().body(JsonNode.class);
            String content=result==null?"":result.path("choices").path(0).path("message").path("content").asText("");
            if(content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"模型没有返回有效回答，请稍后重试。 ");
            return content;
        } catch(RestClientResponseException e) {
            if(e.getStatusCode().value()==402) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"DeepSeek 账户余额不足，请检查账户余额。 ");
            if(e.getStatusCode().value()==429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"模型访问次数受限，请稍后再试。 ");
            if(e.getStatusCode().value()==401 || e.getStatusCode().value()==403) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"模型密钥无效或无权访问，请联系管理员检查配置。 ");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"模型服务暂不可用，请稍后重试。 ");
        } catch(RestClientException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"模型服务连接失败或超时，请稍后重试。 ");}
    }
}
