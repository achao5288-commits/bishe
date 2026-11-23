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
class UserProfileIntegrationTest {
 @Autowired MockMvc mvc;
 String token(String username) throws Exception {var r=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn();return new ObjectMapper().readTree(r.getResponse().getContentAsByteArray()).get("token").asText();}
 @Test void accountsHaveIsolatedFavoritesAndHistoryAndNoAdminAccess() throws Exception {
  String a=token("profile_a"),b=token("profile_b");
  mvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/me/favorites/1").header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
  mvc.perform(post("/api/me/history/2").header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
  mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.favorites[0]").value(1)).andExpect(jsonPath("$.history[0]").value(2));
  mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+b)).andExpect(status().isOk()).andExpect(jsonPath("$.favorites.length()").value(0)).andExpect(jsonPath("$.history.length()").value(0));
  mvc.perform(get("/api/evaluation/summary").header("Authorization","Bearer "+a)).andExpect(status().isForbidden());
 }
}
