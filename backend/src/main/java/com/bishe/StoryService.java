package com.bishe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StoryService {
    public record Source(String name, String url) {}
    public record Section(String title, String content, List<Source> sources) {}
    public record Story(long attractionId, String title, List<Section> sections) {}
    private final Map<Long,Story> stories;
    StoryService(ObjectMapper mapper) throws IOException {
        try (var input=StoryService.class.getResourceAsStream("/attraction-stories.json")) {
            List<Story> items=mapper.readValue(input,new TypeReference<List<Story>>(){});
            stories=Map.copyOf(items.stream().collect(Collectors.toMap(Story::attractionId,Function.identity())));
        }
    }
    public Story find(long id) {
        Story story=stories.get(id);
        if(story==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"暂无该景点的故事");
        return story;
    }
}
