package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class WeightedRecommendationTest {
 @Autowired MockMvc mvc;
 @Test void multipleInterestsAreExplainedWithWeightedRelationships() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"interestIds\":[\"event-luochuan\",\"person-mao\"],\"limit\":5}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("洛川会议纪念馆"))
   .andExpect(jsonPath("$[0].score").value(110)).andExpect(jsonPath("$[0].graphMatches.length()").value(2))
   .andExpect(jsonPath("$[0].graphMatches[?(@.id == 'event-luochuan')].points").value(org.hamcrest.Matchers.hasItem(60)))
   .andExpect(jsonPath("$[0].graphMatches[?(@.id == 'person-mao')].points").value(org.hamcrest.Matchers.hasItem(50)))
   .andExpect(jsonPath("$[0].graphMatches[?(@.id == 'person-mao')].relationTypes[0]").value(org.hamcrest.Matchers.hasItem("RELATED_TO_PERSON")))
   .andExpect(jsonPath("$[0].reason").value(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("洛川会议"),org.hamcrest.Matchers.containsString("毛泽东"))));
 }
 @Test void twoHopPathShowsItsRelationsAndDiscountedScore() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"interestIds\":[\"work-current\"],\"limit\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].graphMatches[0].relationTypes[0]").value("RELATED_TO_EVENT"))
   .andExpect(jsonPath("$[0].graphMatches[0].relationTypes[1]").value("RELATED_TO_WORK"))
   .andExpect(jsonPath("$[0].graphMatches[0].points").value(23));
 }
}
