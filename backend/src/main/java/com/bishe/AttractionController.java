package com.bishe;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class AttractionController {
    record Recommendation(long id, String name, String region, String theme, String reason, int score, List<GraphService.Match> graphMatches) {}
    record Request(String region, List<String> themes, @Size(max=10) List<@NotNull @Size(max=100) String> interestIds, @NotNull @Min(1) @Max(20) Integer limit) {}
    private final GraphService graph;
    private final StoryService stories;
    AttractionController(GraphService graph, StoryService stories) { this.graph=graph; this.stories=stories; }
    @GetMapping("/attractions") List<GraphService.Attraction> attractions(@RequestParam(required=false) String region) { return graph.attractions(region); }
    @GetMapping("/attractions/{id}/story") StoryService.Story story(@PathVariable long id) { return stories.find(id); }
    @GetMapping("/graph") GraphService.Graph allGraph() { return graph.allGraph(); }
    @GetMapping("/graph/attractions/{id}") GraphService.Graph graph(@PathVariable long id) { return graph.graph(id); }
    @PostMapping("/recommendations") List<Recommendation> recommendations(@Valid @RequestBody Request request) {
        List<String> themes=request.themes()==null?List.of():request.themes();
        List<String> interests=request.interestIds()==null?List.of():request.interestIds().stream().distinct().toList();
        Map<Long,List<GraphService.Match>> matches=graph.matchInterests(request.region(),interests);
        return graph.attractions(request.region()).stream().map(a->{
            List<GraphService.Match> evidence=matches.getOrDefault(a.id(),List.of());
            int score=(themes.contains(a.theme())?20:0)+evidence.stream().mapToInt(m->m.distance()==1?60:30).sum();
            String reason=!evidence.isEmpty()?"与您感兴趣的「"+evidence.get(0).name()+"」存在知识关联。":themes.contains(a.theme())?"符合您选择的主题："+a.theme():"可作为浏览参考。";
            return new Recommendation(a.id(),a.name(),a.region(),a.theme(),reason,score,evidence);
        }).sorted(Comparator.comparingInt(Recommendation::score).reversed().thenComparingLong(Recommendation::id)).limit(request.limit()).toList();
    }
}
