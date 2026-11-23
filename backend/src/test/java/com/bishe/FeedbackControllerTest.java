package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class FeedbackControllerTest {
 @TempDir Path temp;
 @Test void validatesInputsAndExposesOnlyAggregateData() throws Exception {
  var graph=mock(GraphService.class);when(graph.attractions(null)).thenReturn(List.of(new GraphService.Attraction(1,"地点","延安","主题","","","","",List.of())));
  var mvc=MockMvcBuilders.standaloneSetup(new FeedbackController(new FeedbackService(temp.resolve("data.json"),new ObjectMapper()),graph)).build();
  String user="d68b0d7a-53ed-4434-a10f-3473ab4ee248";
  mvc.perform(post("/api/evaluation/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+user+"\",\"attractionId\":1,\"rating\":6}" )).andExpect(status().isBadRequest());
  mvc.perform(post("/api/evaluation/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+user+"\",\"attractionId\":2,\"rating\":5}" )).andExpect(status().isBadRequest());
  mvc.perform(post("/api/evaluation/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+user+"\",\"attractionId\":1,\"rating\":5,\"comment\":\"private text\"}" )).andExpect(status().isNoContent());
  mvc.perform(get("/api/evaluation/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.ratings").value(1)).andExpect(jsonPath("$.judgedPositions").value(0)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private text"))));
 }
 @Test void exposureRatingIsCountedOnlyForTheShownPosition() throws Exception {
  var graph=mock(GraphService.class);when(graph.attractions(null)).thenReturn(List.of(new GraphService.Attraction(1,"地点","延安","主题","","","","",List.of())));
  var mvc=MockMvcBuilders.standaloneSetup(new FeedbackController(new FeedbackService(temp.resolve("joined.json"),new ObjectMapper()),graph)).build();
  String user="d68b0d7a-53ed-4434-a10f-3473ab4ee248";
  var result=mvc.perform(post("/api/evaluation/exposures").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+user+"\",\"attractionIds\":[1]}")).andExpect(status().isOk()).andReturn();
  String exposure=new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(post("/api/evaluation/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+user+"\",\"attractionId\":1,\"exposureId\":\""+exposure+"\",\"rating\":2}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/evaluation/summary")).andExpect(jsonPath("$.exposures").value(1)).andExpect(jsonPath("$.judgedPositions").value(1)).andExpect(jsonPath("$.positivePositions").value(0)).andExpect(jsonPath("$.observedPositiveRate").value(0.0));
 }
}
