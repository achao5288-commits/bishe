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
class DiversifiedRecommendationTest {
 @Autowired MockMvc mvc;
 @Test void alreadyVisitedSitesArePenalizedAndStillAvailable() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"favoriteIds\":[1],\"historyIds\":[2,1],\"limit\":20}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.isOneOf(1,2))))
   .andExpect(jsonPath("$[?(@.id == 1)].repeatPenalty").value(org.hamcrest.Matchers.hasItem(50)))
   .andExpect(jsonPath("$[?(@.id == 2)].repeatPenalty").value(org.hamcrest.Matchers.hasItem(30)));
 }
 @Test void variedThemesArePreferredAtEqualRelevance() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"limit\":3}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[1].id").value(3))
   .andExpect(jsonPath("$[1].theme").value("红色地标"));
 }
 @Test void historyIdsMustBePositive() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"historyIds\":[0],\"limit\":3}")).andExpect(status().isBadRequest());
 }
 @Test void strongInterestCanOutweighRepeatPenalty() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"延安\",\"interestIds\":[\"event-luochuan\"],\"favoriteIds\":[13,13],\"limit\":1}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(13)).andExpect(jsonPath("$[0].score").value(60))
   .andExpect(jsonPath("$[0].repeatPenalty").value(50)).andExpect(jsonPath("$[0].rerankScore").value(10));
 }
}
