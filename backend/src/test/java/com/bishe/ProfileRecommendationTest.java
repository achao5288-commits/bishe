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
class ProfileRecommendationTest {
 @Autowired MockMvc mvc;
 @Test void profilePromotesRelevantAttractionAndExplainsContribution() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"profileInterests\":{\"event-luochuan\":3},\"limit\":3}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("洛川会议纪念馆"))
   .andExpect(jsonPath("$[0].profileScore").value(18)).andExpect(jsonPath("$[0].score").value(18))
   .andExpect(jsonPath("$[0].profileMatches[0].name").value("洛川会议"));
 }
 @Test void explicitInterestDoesNotDoubleCountProfile() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"interestIds\":[\"event-luochuan\"],\"profileInterests\":{\"event-luochuan\":3},\"limit\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].score").value(60)).andExpect(jsonPath("$[0].profileScore").value(0));
 }
 @Test void invalidWeightIsRejected() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"profileInterests\":{\"event-luochuan\":4},\"limit\":3}")).andExpect(status().isBadRequest());
 }
}
