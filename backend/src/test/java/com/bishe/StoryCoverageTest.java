package com.bishe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class StoryCoverageTest {
 @Test void everySeedAttractionHasSourcedSections() throws Exception {
  var mapper=new ObjectMapper();
  JsonNode seed=mapper.readTree(getClass().getResourceAsStream("/graph-seed.json"));
  JsonNode stories=mapper.readTree(getClass().getResourceAsStream("/attraction-stories.json"));
  Set<Long> ids=new HashSet<>(), storyIds=new HashSet<>();
  seed.path("attractions").forEach(n->assertTrue(ids.add(n.path("id").asLong())));
  for(JsonNode story:stories) {
   assertTrue(storyIds.add(story.path("attractionId").asLong()),"duplicate story");
   assertFalse(story.path("title").asText().isBlank());
   assertTrue(story.path("sections").size()>=3);
   for(JsonNode section:story.path("sections")) {
    assertFalse(section.path("title").asText().isBlank());
    assertFalse(section.path("content").asText().isBlank());
    assertFalse(section.path("sources").isEmpty());
    for(JsonNode source:section.path("sources")) {
     assertFalse(source.path("name").asText().isBlank());
     assertTrue(source.path("url").asText().startsWith("https://"));
    }
   }
  }
  assertEquals(ids,storyIds,"every attraction must have a corresponding story");
 }
}
