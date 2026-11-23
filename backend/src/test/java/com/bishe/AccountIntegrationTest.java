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
class AccountIntegrationTest {
 @Autowired MockMvc mvc; @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Test void registrationCreatesUserAndToken() throws Exception {
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"user_a12\",\"password\":\"password123\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.role").value("USER"));
 }
 @Test void credentialsAreHashedAndInvalidLoginAndDuplicateRegistrationAreRejected() throws Exception {
  String body="{\"username\":\"credential_test\",\"password\":\"password123\"}";
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  String stored=jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,"credential_test");
  org.junit.jupiter.api.Assertions.assertNotEquals("password123",stored);
  org.junit.jupiter.api.Assertions.assertTrue(stored.startsWith("$2"));
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"credential_test\",\"password\":\"incorrectpass\"}")).andExpect(status().isUnauthorized());
 }
}
