package com.bishe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ContentAdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired GraphService graph;

    @Test @WithMockUser(roles="USER")
    void ordinaryUserCannotManageContent() throws Exception {
        mvc.perform(get("/api/admin/content/attractions")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/content/attractions/99999991")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles="ADMIN")
    void administratorCanManageAttractionStoryAndGraphRelationship() throws Exception {
        String place = """
          {"id":99999991,"name":"测试景点","region":"延安","theme":"纪念馆","description":"测试描述","image":"/images/test.png","sourceName":"测试资料","sourceUrl":"https://example.org/source","aliases":["测试别名"]}
          """;
        String story = """
          {"attractionId":99999991,"title":"测试故事","sections":[{"title":"背景","content":"根据资料整理的背景。","sources":[{"name":"测试来源","url":"https://example.org/story"}]}]}
          """;
        String entity = """
          {"id":"event-admin-test-99999991","name":"测试事件","kind":"Event","sourceUrl":"https://example.org/event"}
          """;
        String relation = """
          {"source":"attraction-99999991","target":"event-admin-test-99999991","type":"RELATED_TO_EVENT","sourceUrl":"https://example.org/relation"}
          """;
        try {
            mvc.perform(put("/api/admin/content/attractions/99999991").contentType(MediaType.APPLICATION_JSON).content(place)).andExpect(status().isOk());
            mvc.perform(put("/api/admin/content/stories/99999991").contentType(MediaType.APPLICATION_JSON).content(story)).andExpect(status().isOk());
            mvc.perform(get("/api/attractions/99999991/story")).andExpect(status().isOk()).andExpect(jsonPath("$.sections[0].sources[0].url").value("https://example.org/story"));
            mvc.perform(put("/api/admin/content/entities/event-admin-test-99999991").contentType(MediaType.APPLICATION_JSON).content(entity)).andExpect(status().isOk());
            mvc.perform(put("/api/admin/content/relations").contentType(MediaType.APPLICATION_JSON).content(relation)).andExpect(status().isOk());
            mvc.perform(get("/api/graph/attractions/99999991")).andExpect(status().isOk()).andExpect(jsonPath("$.edges[?(@.target == 'event-admin-test-99999991')]").exists());
            mvc.perform(put("/api/admin/content/attractions/99999991").contentType(MediaType.APPLICATION_JSON).content(place.replace("测试景点","修改后的景点"))).andExpect(status().isOk());
            mvc.perform(put("/api/admin/content/stories/99999991").contentType(MediaType.APPLICATION_JSON).content(story.replace("测试故事","修改后的故事"))).andExpect(status().isOk());
            mvc.perform(put("/api/admin/content/entities/event-admin-test-99999991").contentType(MediaType.APPLICATION_JSON).content(entity.replace("测试事件","修改后的事件"))).andExpect(status().isOk());
            mvc.perform(put("/api/admin/content/relations").contentType(MediaType.APPLICATION_JSON).content(relation.replace("example.org/relation","example.org/updated"))).andExpect(status().isOk());
            graph.seed();
            mvc.perform(get("/api/attractions/99999991/story")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("修改后的故事"));
            mvc.perform(get("/api/graph/attractions/99999991")).andExpect(status().isOk()).andExpect(jsonPath("$.edges[?(@.sourceUrl == 'https://example.org/updated')]").exists());
            mvc.perform(delete("/api/admin/content/relations").contentType(MediaType.APPLICATION_JSON).content(relation)).andExpect(status().isNoContent());
            mvc.perform(delete("/api/admin/content/stories/99999991")).andExpect(status().isNoContent());
            graph.seed();
            mvc.perform(get("/api/attractions/99999991/story")).andExpect(status().isNotFound());
        } finally {
            mvc.perform(delete("/api/admin/content/attractions/99999991"));
            mvc.perform(delete("/api/admin/content/entities/event-admin-test-99999991"));
        }
    }
    @Test @WithMockUser(roles="ADMIN")
    void rejectsUnsafeSourcesAndInvalidRelationshipTypes() throws Exception {
        String place="""
          {"id":99999992,"name":"测试景点","region":"延安","theme":"纪念馆","description":"测试","image":"/images/test.png","sourceName":"来源","sourceUrl":"javascript:alert(1)","aliases":[]}
          """;
        mvc.perform(put("/api/admin/content/attractions/99999992").contentType(MediaType.APPLICATION_JSON).content(place)).andExpect(status().isBadRequest());
        String relation="""
          {"source":"attraction-1","target":"event-test","type":"EVIL` MATCH (n) DETACH DELETE n //","sourceUrl":"https://example.org"}
          """;
        mvc.perform(put("/api/admin/content/relations").contentType(MediaType.APPLICATION_JSON).content(relation)).andExpect(status().isBadRequest());
    }
}
