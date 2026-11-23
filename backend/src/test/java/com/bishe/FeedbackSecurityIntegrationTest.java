package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class FeedbackSecurityIntegrationTest {
 @Autowired MockMvc mvc; final ObjectMapper json=new ObjectMapper();
 @Test void feedbackCannotImpersonateAnotherAccount() throws Exception {
  var ar=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"feedback_a\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn();
  var br=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"feedback_b\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn();
  var a=json.readTree(ar.getResponse().getContentAsByteArray());var b=json.readTree(br.getResponse().getContentAsByteArray());
  String uid=a.get("userId").asText(),at=a.get("token").asText(),bt=b.get("token").asText();
  var er=mvc.perform(post("/api/evaluation/exposures").header("Authorization","Bearer "+at).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+uid+"\",\"attractionIds\":[1]}" )).andExpect(status().isOk()).andReturn();
  String exposure=json.readTree(er.getResponse().getContentAsByteArray()).get("id").asText();
  mvc.perform(post("/api/evaluation/feedback").header("Authorization","Bearer "+bt).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\""+uid+"\",\"attractionId\":1,\"exposureId\":\""+exposure+"\",\"rating\":5}" )).andExpect(status().isBadRequest());
 }
}
