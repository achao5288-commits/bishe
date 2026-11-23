package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ExpandedGraphTest {
 @Autowired MockMvc mvc;
 @Test void includesYulinSitesWithDistinctIllustrations() throws Exception {
   mvc.perform(get("/api/attractions").param("region", "榆林"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].name").value("杨家沟革命旧址"))
      .andExpect(jsonPath("$[1].name").value("小河会议旧址"));
 }
 @Test void exposesPeopleWorksAndEventsAcrossTheGraph() throws Exception {
   mvc.perform(get("/api/graph"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.nodes[?(@.name == '毛泽东' && @.label == '人物')]").exists())
      .andExpect(jsonPath("$.nodes[?(@.name == '在延安文艺座谈会上的讲话' && @.label == '著作')]").exists())
      .andExpect(jsonPath("$.edges[?(@.type == 'PARTICIPATED_IN')]").exists());
 }
 @Test void localGraphIncludesSourcedPerson() throws Exception {
   mvc.perform(get("/api/graph/attractions/7"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.nodes[?(@.name == '毛泽东' && @.label == '人物')]").exists())
      .andExpect(jsonPath("$.edges[?(@.type == 'RELATED_TO_PERSON')]").exists());
 }
}

