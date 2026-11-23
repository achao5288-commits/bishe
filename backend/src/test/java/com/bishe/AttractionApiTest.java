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
class AttractionApiTest {
 @Autowired MockMvc mvc;
 @Test void filtersAttractionsByRegion() throws Exception {
   mvc.perform(get("/api/attractions").param("region", "延安"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].region").value("延安"));
 }
 @Test void ranksMatchingThemeAndExplainsIt() throws Exception {
   mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON)
      .content("{\"region\":\"延安\",\"themes\":[\"革命旧址\"],\"limit\":5}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("示例革命旧址"))
      .andExpect(jsonPath("$[0].reason").value("符合您选择的主题：革命旧址"));
 }
 @Test void validatesLimit() throws Exception {
   mvc.perform(post("/api/recommendations").contentType(MediaType.APPLICATION_JSON)
      .content("{\"limit\":0}")) .andExpect(status().isBadRequest());
 }
}
