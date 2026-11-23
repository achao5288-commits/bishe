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
@SpringBootTest(properties={"APP_ADMIN_USERNAME=admin_unit","APP_ADMIN_PASSWORD=administrator_test_password"}) @AutoConfigureMockMvc
class AdminAccessIntegrationTest {
 @Autowired MockMvc mvc;
 @Test void configuredAdminCanAccessSummary() throws Exception {
  var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin_unit\",\"password\":\"administrator_test_password\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn();
  String token=new ObjectMapper().readTree(r.getResponse().getContentAsByteArray()).get("token").asText();
  mvc.perform(get("/api/evaluation/summary").header("Authorization","Bearer "+token)).andExpect(status().isOk());
 }
}
