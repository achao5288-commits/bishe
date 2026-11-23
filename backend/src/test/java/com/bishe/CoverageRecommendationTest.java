package com.bishe;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class CoverageRecommendationTest {
 @Autowired MockMvc mvc;
 private List<Long> ids(String history) throws Exception {
  var result=mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON).content("{\"region\":\"榆林\",\"historyIds\":"+history+",\"limit\":5}")).andExpect(status().isOk()).andReturn();
  var root=new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
  var ids=new java.util.ArrayList<Long>();root.forEach(x->ids.add(x.get("id").asLong()));return ids;
 }
 @Test void differentHistoryDistributesTiedCandidatesButSameHistoryIsStable() throws Exception {
  var one=ids("[1]");assertEquals(one,ids("[1,1]"));assertNotEquals(one,ids("[2]"));
 }
}
