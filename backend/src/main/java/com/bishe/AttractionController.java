package com.bishe;

import java.util.Comparator;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttractionController {
    record Attraction(long id, String name, String region, String theme, String description) {}
    record Recommendation(long id, String name, String region, String theme, String reason) {}
    record Request(String region, List<String> themes, @NotNull @Min(1) @Max(20) Integer limit) {}

    private static final List<Attraction> DEMO = List.of(
        new Attraction(1, "示例革命旧址", "延安", "革命旧址", "虚构演示景点，非历史资料。"),
        new Attraction(2, "示例纪念馆", "延安", "纪念馆", "虚构演示景点，非历史资料。"),
        new Attraction(3, "示例红色纪念地", "榆林", "革命旧址", "虚构演示景点，非历史资料。")
    );
    private List<Attraction> inRegion(String region) {
        return DEMO.stream().filter(a -> region == null || region.isBlank() || a.region().equals(region)).toList();
    }
    @GetMapping("/attractions")
    List<Attraction> attractions(@RequestParam(required=false) String region) { return inRegion(region); }
    @PostMapping("/recommendations")
    List<Recommendation> recommendations(@Valid @RequestBody Request request) {
        List<String> themes = request.themes() == null ? List.of() : request.themes();
        return inRegion(request.region()).stream()
            .sorted(Comparator.comparing((Attraction a) -> themes.contains(a.theme())).reversed())
            .limit(request.limit())
            .map(a -> new Recommendation(a.id(), a.name(), a.region(), a.theme(), themes.contains(a.theme())
                ? "符合您选择的主题：" + a.theme() : "符合您选择的地区，可作为浏览参考。"))
            .toList();
    }
}
