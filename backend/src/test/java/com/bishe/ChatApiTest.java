package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ChatApiTest {
 @Autowired MockMvc mvc;
 @MockitoBean LlmGateway llm;
 @Test void answersWithVerifiedStoryAndGraphSources() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("杨家岭见证了文艺座谈会和中共七大。");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"请讲杨家岭的完整故事\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("answered"))
    .andExpect(jsonPath("$.answer").value("杨家岭见证了文艺座谈会和中共七大。"))
    .andExpect(jsonPath("$.attractions[0].name").value("杨家岭革命旧址"))
    .andExpect(jsonPath("$.sources[0].url").isNotEmpty())
    .andExpect(jsonPath("$.graphEdges[?(@.type == 'RELATED_TO_EVENT')]").exists());
   var prompt=org.mockito.ArgumentCaptor.forClass(String.class);
   verify(llm).generate(anyString(),prompt.capture());
   org.junit.jupiter.api.Assertions.assertTrue(prompt.getValue().contains("1945年4月23日"));
   org.junit.jupiter.api.Assertions.assertTrue(prompt.getValue().contains("RELATED_TO_EVENT"));
   org.junit.jupiter.api.Assertions.assertTrue(prompt.getValue().contains("中国共产党第七次全国代表大会"));
   org.junit.jupiter.api.Assertions.assertFalse(prompt.getValue().contains("张思德追悼会"),"unrelated two-hop event must not contaminate the selected attraction context");
 }
 @Test void unrecognizedQuestionReturnsInsufficientEvidenceWithoutCallingModel() throws Exception {
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"北京天气如何\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("insufficient_evidence"));
   verifyNoInteractions(llm);
 }
 @Test void selectsAnAttractionUsingExplicitId() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("故事回答");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"请讲完整故事\",\"attractionId\":6}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.attractions[0].name").value("南泥湾革命旧址"));
 }
 @Test void validatesBlankQuestion() throws Exception {
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"  \"}"))
    .andExpect(status().isBadRequest()); verifyNoInteractions(llm);
 }
 @Test void findsAnAttractionFromAnAssociatedPerson() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("张思德的相关故事。");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"张思德的故事\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.attractions[0].name").value("枣园革命旧址"));
 }
 @Test void doesNotGuessCurrentTicketPrices() throws Exception {
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"杨家岭的门票价格是多少\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("insufficient_evidence"));
   verifyNoInteractions(llm);
 }
 @Test void reportsUnavailableModelWithoutExposingInternals() throws Exception {
   when(llm.generate(anyString(),anyString())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"模型密钥未配置"));
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"杨家岭的故事\"}"))
    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("unavailable"))
    .andExpect(jsonPath("$.message").value("模型密钥未配置"));
 }
 @Test void sharedPersonDoesNotMakeUnrelatedSiteMatchAnEvent() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("相关历史。");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"张思德追悼会的经过\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.attractions.length()").value(1))
    .andExpect(jsonPath("$.attractions[0].name").value("枣园革命旧址"));
 }
 @Test void returnedCitationsOnlyUseRetrievedSourceNumbers() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("可靠信息[1]，未经核实的引用[999]。");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"杨家岭的故事\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("可靠信息[1]，未经核实的引用（引用未核实）。"))
    .andExpect(jsonPath("$.sources[0].id").value(1)).andExpect(jsonPath("$.citationWarning").value(true));
   var prompt=org.mockito.ArgumentCaptor.forClass(String.class); verify(llm).generate(anyString(),prompt.capture());
   org.junit.jupiter.api.Assertions.assertTrue(prompt.getValue().contains("来源编号[1]"));
 }
}
