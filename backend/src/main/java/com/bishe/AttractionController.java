package com.bishe;
import java.util.Comparator;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class AttractionController {
    record Recommendation(long id, String name, String region, String theme, String reason) {}
    record Request(String region, List<String> themes, @NotNull @Min(1) @Max(20) Integer limit) {}
    private final GraphService graph;
    AttractionController(GraphService graph) { this.graph=graph; }
    @GetMapping("/attractions") List<GraphService.Attraction> attractions(@RequestParam(required=false) String region) { return graph.attractions(region); }
    @GetMapping("/graph") GraphService.Graph allGraph() { return graph.allGraph(); }
    @GetMapping("/graph/attractions/{id}") GraphService.Graph graph(@PathVariable long id) { return graph.graph(id); }
    @PostMapping("/recommendations") List<Recommendation> recommendations(@Valid @RequestBody Request request) {
        List<String> themes=request.themes()==null?List.of():request.themes();
        return graph.attractions(request.region()).stream().sorted(Comparator.comparing((GraphService.Attraction a)->themes.contains(a.theme())).reversed()).limit(request.limit())
            .map(a->new Recommendation(a.id(),a.name(),a.region(),a.theme(),themes.contains(a.theme())?"符合您选择的主题："+a.theme():"符合您选择的地区，可作为浏览参考。" )).toList();
    }
}

