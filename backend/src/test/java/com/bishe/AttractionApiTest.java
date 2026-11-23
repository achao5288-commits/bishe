package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AttractionApiTest {
 @Autowired MockMvc mvc;
 @Test void listsRealAttractionsWithSourcesAndDifferentIllustrations() throws Exception {
   mvc.perform(get("/api/attractions").param("region", "延安"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6))
      .andExpect(jsonPath("$[0].name").value("杨家岭革命旧址"))
      .andExpect(jsonPath("$[0].sourceUrl").isNotEmpty())
      .andExpect(jsonPath("$[0].image").value("/images/yangjialing.png"))
      .andExpect(jsonPath("$[1].image").value("/images/zaoyuan.png"));
 }
 @Test void ranksMatchingTheme() throws Exception {
   mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON)
      .content("{\"region\":\"延安\",\"themes\":[\"革命旧址\"],\"limit\":5}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("杨家岭革命旧址"))
      .andExpect(jsonPath("$[0].reason").value("符合您选择的主题：革命旧址"));
 }
 @Test void graphExposesSourcedEventRelationship() throws Exception {
   mvc.perform(get("/api/graph/attractions/1"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.nodes[?(@.name == '中国共产党第七次全国代表大会')]").exists())
      .andExpect(jsonPath("$.edges[?(@.type == 'RELATED_TO_EVENT')]").exists());
 }
 @Test void graphRejectsUnknownAttraction() throws Exception { mvc.perform(get("/api/graph/attractions/999")).andExpect(status().isNotFound()); }
 @Test void additionalAttractionsIncludeDistinctImagesAndSources() throws Exception {
   mvc.perform(get("/api/attractions").param("region", "延安"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[3].name").value("延安革命纪念馆"))
      .andExpect(jsonPath("$[3].image").value("/images/memorial.png"))
      .andExpect(jsonPath("$[4].name").value("王家坪革命旧址"))
      .andExpect(jsonPath("$[5].name").value("南泥湾革命旧址"));
 }
 @Test void newGraphIncludesNanniwanEvent() throws Exception {
   mvc.perform(get("/api/graph/attractions/6"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.nodes[?(@.name == '南泥湾大生产运动')]").exists());
 }
 @Test void validatesLimit() throws Exception { mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"limit\":0}")).andExpect(status().isBadRequest()); }
}

