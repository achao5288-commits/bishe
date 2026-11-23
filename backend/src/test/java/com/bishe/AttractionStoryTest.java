package com.bishe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class AttractionStoryTest {
 @Autowired MockMvc mvc;
 @Test void eachAttractionHasThreeSourcedStorySections() throws Exception {
  for(int id=1;id<=12;id++) mvc.perform(get("/api/attractions/"+id+"/story"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.attractionId").value(id))
   .andExpect(jsonPath("$.sections.length()").value(3))
   .andExpect(jsonPath("$.sections[0].title").isNotEmpty())
   .andExpect(jsonPath("$.sections[0].content").isNotEmpty())
   .andExpect(jsonPath("$.sections[0].sources[0].url").isNotEmpty())
   .andExpect(jsonPath("$.sections[1].sources[0].url").isNotEmpty())
   .andExpect(jsonPath("$.sections[2].sources[0].url").isNotEmpty());
 }
 @Test void missingStoryReturnsNotFound() throws Exception { mvc.perform(get("/api/attractions/999/story")).andExpect(status().isNotFound()); }
 @Test void yangjialingNarrativeCoversBothKeyEvents() throws Exception {
  mvc.perform(get("/api/attractions/1/story"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.sections[1].content").value(org.hamcrest.Matchers.containsString("文艺座谈会")))
    .andExpect(jsonPath("$.sections[2].content").value(org.hamcrest.Matchers.containsString("七大")));
 }
}
