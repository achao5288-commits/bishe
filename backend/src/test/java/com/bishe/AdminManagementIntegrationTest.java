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
class AdminManagementIntegrationTest {
 @Autowired MockMvc mvc;final ObjectMapper json=new ObjectMapper();
 @Test void roleChangesApplyToExistingTokensAndSelfDemotionIsRejected() throws Exception {
  var aa=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin_unit\",\"password\":\"administrator_test_password\"}")).andExpect(status().isOk()).andReturn();
  var uu=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"promote_unit\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn();
  var a=json.readTree(aa.getResponse().getContentAsByteArray());var u=json.readTree(uu.getResponse().getContentAsByteArray());String at=a.get("token").asText(),ut=u.get("token").asText(),uid=u.get("userId").asText();
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+at)).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.username == 'admin_unit')].protectedUser").value(org.hamcrest.Matchers.hasItem(true)));
  mvc.perform(put("/api/admin/users/"+uid+"/role").header("Authorization","Bearer "+at).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/evaluation/summary").header("Authorization","Bearer "+ut)).andExpect(status().isOk());
  mvc.perform(put("/api/admin/users/"+uid+"/role").header("Authorization","Bearer "+at).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/evaluation/summary").header("Authorization","Bearer "+ut)).andExpect(status().isForbidden());
  mvc.perform(put("/api/admin/users/"+a.get("userId").asText()+"/role").header("Authorization","Bearer "+at).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}")).andExpect(status().isBadRequest());
 }
}
