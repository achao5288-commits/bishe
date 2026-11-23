package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class DeepSeekGatewayTest {
 @Test void sendsGroundedPromptToDeepSeekAndParsesResponse() {
   var builder=RestClient.builder().baseUrl("https://api.deepseek.com");
   var server=MockRestServiceServer.bindTo(builder).build();
   server.expect(requestTo("https://api.deepseek.com/chat/completions")).andExpect(method(HttpMethod.POST))
    .andExpect(header("Authorization","Bearer test-key"))
    .andExpect(jsonPath("$.model").value("deepseek-flash"))
    .andExpect(jsonPath("$.thinking.type").value("disabled"))
    .andExpect(jsonPath("$.messages[1].content").value("资料与问题"))
    .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"根据资料的回答\"}}]}",MediaType.APPLICATION_JSON));
   var gateway=new DeepSeekGateway("test-key",builder.build());
   assertEquals("根据资料的回答",gateway.generate("规则","资料与问题")); server.verify();
 }
 @Test void noKeyIsExplicitlyUnavailableWithoutRequest() {
   var gateway=new DeepSeekGateway("",RestClient.create());
   var error=assertThrows(ResponseStatusException.class,()->gateway.generate("规则","问题"));
   assertEquals(503,error.getStatusCode().value()); assertTrue(error.getReason().contains("密钥"));
 }
 @Test void rateLimitIsReportedWithoutProviderBody() {
   var builder=RestClient.builder().baseUrl("https://api.deepseek.com");
   var server=MockRestServiceServer.bindTo(builder).build();
   server.expect(requestTo("https://api.deepseek.com/chat/completions")).andRespond(withTooManyRequests().body("secret-provider-detail"));
   var gateway=new DeepSeekGateway("test-key",builder.build());
   var error=assertThrows(ResponseStatusException.class,()->gateway.generate("规则","问题"));
   assertEquals(429,error.getStatusCode().value()); assertFalse(error.getReason().contains("secret-provider-detail")); server.verify();
 }
 @Test void insufficientBalanceIsReportedClearly() {
   var builder=RestClient.builder().baseUrl("https://api.deepseek.com");
   var server=MockRestServiceServer.bindTo(builder).build();
   server.expect(requestTo("https://api.deepseek.com/chat/completions")).andRespond(withStatus(org.springframework.http.HttpStatus.PAYMENT_REQUIRED));
   var gateway=new DeepSeekGateway("test-key",builder.build());
   var error=assertThrows(ResponseStatusException.class,()->gateway.generate("规则","问题"));
   assertEquals(503,error.getStatusCode().value()); assertTrue(error.getReason().contains("余额")); server.verify();
 }
}
