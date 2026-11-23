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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class MoreAttractionsTest {
 @Autowired MockMvc mvc; @MockitoBean LlmGateway llm;
 @Test void includesFourNewSourcedAttractions() throws Exception {
   mvc.perform(get("/api/attractions"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.name == '凤凰山革命旧址')].image").value(org.hamcrest.Matchers.hasItem("/images/fenghuangshan.png")))
    .andExpect(jsonPath("$[?(@.name == '瓦窑堡会议旧址')]").exists())
    .andExpect(jsonPath("$[?(@.name == '吴起中央红军长征胜利纪念园')]").exists())
    .andExpect(jsonPath("$[?(@.name == '神泉堡革命纪念馆')]").exists());
 }
 @Test void newSitesHaveStoriesAndGraph() throws Exception {
   for(int id=9;id<=12;id++) {
    mvc.perform(get("/api/attractions/"+id+"/story")).andExpect(status().isOk()).andExpect(jsonPath("$.sections.length()").value(3)).andExpect(jsonPath("$.sections[0].sources[0].url").isNotEmpty());
    mvc.perform(get("/api/graph/attractions/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.edges[?(@.type == 'RELATED_TO_EVENT')]").exists());
   }
 }
 @Test void chatRecognizesNewPlaceAliases() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("根据资料整理的凤凰山故事。");
   mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"请讲凤凰山的故事\"}"))
     .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("answered"))
     .andExpect(jsonPath("$.attractions[0].name").value("凤凰山革命旧址"));
   verify(llm).generate(anyString(),contains("凤凰山"));
 }
 @Test void graphSupportsRecommendationFromNewEvent() throws Exception {
   mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"interestIds\":[\"event-wayaobao\"],\"limit\":3}"))
     .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("瓦窑堡会议旧址"))
     .andExpect(jsonPath("$[0].graphMatches[0].sources[0]").isNotEmpty());
 }
 @Test void newPlaceAliasesResolveToTheirOwnStories() throws Exception {
   when(llm.generate(anyString(),anyString())).thenReturn("相关故事。");
   String[] queries={"瓦窑堡会议的故事","吴起纪念园的故事","神泉堡的故事"};
   String[] names={"瓦窑堡会议旧址","吴起中央红军长征胜利纪念园","神泉堡革命纪念馆"};
   for(int i=0;i<queries.length;i++) mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
      .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("question",queries[i]))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("answered"))
      .andExpect(jsonPath("$.attractions.length()").value(1)).andExpect(jsonPath("$.attractions[0].name").value(names[i]));
 }
}
