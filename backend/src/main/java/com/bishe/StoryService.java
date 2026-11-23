package com.bishe;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoryService {
    public record Source(String name, String url) {}
    public record Section(String title, String content, List<Source> sources) {}
    public record Story(long attractionId, String title, List<Section> sections) {}
    private final GraphService graph;
    private final ObjectMapper mapper;
    StoryService(GraphService graph, ObjectMapper mapper) { this.graph=graph; this.mapper=mapper; }
    public Story find(long id) {
        try (var session=graph.driver().session()) {
            String json=session.executeRead(tx -> tx.run("MATCH (a:Attraction {id:$id}) WHERE a.storyJson IS NOT NULL RETURN a.storyJson AS story",Map.of("id",id)).list(r->r.get("story").asString()).stream().findFirst().orElse(null));
            if(json==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"暂无该景点的故事");
            try { return mapper.readValue(json,Story.class); }
            catch (java.io.IOException e) { throw new IllegalStateException("故事资料格式错误",e); }
        }
    }
}
