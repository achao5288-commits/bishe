package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class NewHeritageTest {
 @Autowired MockMvc mvc; @MockitoBean LlmGateway llm;
 @Test void fourSitesHaveStoriesAndSourcedGraphEvents() throws Exception {
  for(int id=13;id<=16;id++) {
   mvc.perform(get("/api/attractions/"+id+"/story")).andExpect(status().isOk()).andExpect(jsonPath("$.sections.length()").value(3)).andExpect(jsonPath("$.sections[0].sources[0].url").isNotEmpty());
   mvc.perform(get("/api/graph/attractions/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.edges[?(@.type == 'RELATED_TO_EVENT')]").exists());
  }
 }
 @Test void aliasesRetrieveTheCorrectEvidenceForChat() throws Exception {
  when(llm.generate(anyString(),anyString())).thenReturn("根据资料整理的历史故事。[1]");
  String[] questions={"洛川会议的故事","绥德纪念馆的故事","郝家桥的故事","刘志丹陵园的故事"};
  String[] names={"洛川会议纪念馆","绥德县革命历史纪念馆","郝家桥红色革命教育基地","刘志丹烈士陵园"};
  for(int i=0;i<questions.length;i++) mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("question",questions[i])))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("answered")).andExpect(jsonPath("$.attractions[0].name").value(names[i])).andExpect(jsonPath("$.sources[0].url").isNotEmpty());
  verify(llm,atLeastOnce()).generate(anyString(),contains("1943年"));
 }
 @Test void eventInterestRecommendsLuochuan() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"interestIds\":[\"event-luochuan\"],\"limit\":3}")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("洛川会议纪念馆")).andExpect(jsonPath("$[0].graphMatches[0].sources[0]").isNotEmpty());
 }
}
