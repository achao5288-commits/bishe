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
class GraphRecommendationTest {
 @Autowired MockMvc mvc;
 @Test void directEventInterestChangesRankingAndReturnsEvidence() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"themes\":[\"革命旧址\"],\"interestIds\":[\"event-xiaohe\"],\"limit\":5}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("小河会议旧址"))
    .andExpect(jsonPath("$[0].graphMatches[0].name").value("小河会议"))
    .andExpect(jsonPath("$[0].graphMatches[0].distance").value(1))
    .andExpect(jsonPath("$[0].graphMatches[0].sources[0]").value("https://zhuanti.mct.gov.cn/history_detail/782.html"));
 }
 @Test void matchesAWorkThroughTwoHopEvent() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"interestIds\":[\"work-current\"],\"limit\":2}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("杨家沟革命旧址"))
    .andExpect(jsonPath("$[0].graphMatches[0].distance").value(2))
    .andExpect(jsonPath("$[0].graphMatches[0].path[1]").value("十二月会议"))
    .andExpect(jsonPath("$[0].graphMatches[0].sources.length()").value(2));
 }
 @Test void unknownInterestDoesNotInventEvidence() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"interestIds\":[\"missing\"],\"limit\":2}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("杨家沟革命旧址"))
    .andExpect(jsonPath("$[0].graphMatches.length()").value(0)).andExpect(jsonPath("$[0].score").value(0));
 }
 @Test void repeatedInterestsAreNotCountedTwice() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"interestIds\":[\"event-xiaohe\",\"event-xiaohe\"],\"limit\":2}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$[0].score").value(60)).andExpect(jsonPath("$[0].graphMatches.length()").value(1));
 }
 @Test void tooManyInterestsAreRejected() throws Exception {
  mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"interestIds\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\"],\"limit\":2}"))
    .andExpect(status().isBadRequest());
 }
}
