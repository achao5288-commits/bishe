package com.bishe;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
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
    record Recommendation(long id, String name, String region, String theme, String reason, int score, List<GraphService.Match> graphMatches, int profileScore, List<GraphService.Match> profileMatches, int repeatPenalty, int diversityPenalty, int rerankScore) {}
    record Request(String region, List<String> themes, @Size(max=10) List<@NotNull @Size(max=100) String> interestIds, @Size(max=10) Map<@NotBlank @Size(max=100) String,@NotNull @Min(1) @Max(3) Integer> profileInterests, @Size(max=100) List<@NotNull @Min(1) Long> favoriteIds, @Size(max=50) List<@NotNull @Min(1) Long> historyIds, @NotNull @Min(1) @Max(20) Integer limit) {}
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
        Map<String,Integer> profile=request.profileInterests()==null?Map.of():request.profileInterests();
        List<String> allInterests=new ArrayList<>(interests);
        profile.keySet().stream().filter(id->!interests.contains(id)).sorted().forEach(allInterests::add);
        Map<Long,List<GraphService.Match>> matches=graph.matchInterests(request.region(),allInterests);
        List<Recommendation> candidates=graph.attractions(request.region()).stream().map(a->{
            List<GraphService.Match> found=matches.getOrDefault(a.id(),List.of());
            List<GraphService.Match> evidence=found.stream().filter(m->interests.contains(m.id())).toList();
            List<GraphService.Match> profileMatches=found.stream().filter(m->!interests.contains(m.id()) && profile.containsKey(m.id())).toList();
            int profileScore=profileMatches.stream().mapToInt(m->m.points()*profile.get(m.id())/10).sum();
            int score=(themes.contains(a.theme())?20:0)+evidence.stream().mapToInt(GraphService.Match::points).sum()+profileScore;
            String reason=!evidence.isEmpty()?"关联您感兴趣的「"+evidence.stream().map(GraphService.Match::name).collect(java.util.stream.Collectors.joining("、"))+"」，可查看下方路径和来源。":!profileMatches.isEmpty()?"根据您收藏或浏览过的景点，发现与「"+profileMatches.stream().map(GraphService.Match::name).collect(java.util.stream.Collectors.joining("、"))+"」有关的内容。":themes.contains(a.theme())?"符合您选择的主题："+a.theme():"可作为浏览参考。";
            return new Recommendation(a.id(),a.name(),a.region(),a.theme(),reason,score,evidence,profileScore,profileMatches,0,0,score);
        }).toList();
        return rerank(candidates,request,matches);
    }
    private List<Recommendation> rerank(List<Recommendation> candidates, Request request, Map<Long,List<GraphService.Match>> matches) {
        Set<Long> favorites=new HashSet<>(request.favoriteIds()==null?List.of():request.favoriteIds());
        Set<Long> history=new HashSet<>(request.historyIds()==null?List.of():request.historyIds());
        List<Recommendation> remaining=new ArrayList<>(candidates), result=new ArrayList<>();
        Set<String> matchedIds=matches.values().stream().flatMap(List::stream).map(GraphService.Match::id).collect(java.util.stream.Collectors.toSet());
        List<String> selected=request.interestIds()==null?List.of():request.interestIds().stream().filter(matchedIds::contains).distinct().sorted().toList();
        Map<String,Integer> weights=new TreeMap<>();
        if(request.profileInterests()!=null)request.profileInterests().forEach((id,weight)->{if(matchedIds.contains(id) && !selected.contains(id))weights.put(id,weight);});
        boolean personalized=!favorites.isEmpty() || !history.isEmpty() || !selected.isEmpty() || !weights.isEmpty();
        String signature=favorites.stream().sorted().toList()+"|"+history.stream().sorted().toList()+"|"+selected+"|"+weights;
        Map<String,Integer> themes=new HashMap<>(),regions=new HashMap<>();
        while(!remaining.isEmpty() && result.size()<request.limit()) {
            List<Recommendation> scored=remaining.stream().map(item->{
                int repeat=favorites.contains(item.id())?50:history.contains(item.id())?30:0;
                int diversity=8*Math.min(themes.getOrDefault(item.theme(),0),2)+3*Math.min(regions.getOrDefault(item.region(),0),2);
                return new Recommendation(item.id(),item.name(),item.region(),item.theme(),item.reason(),item.score(),item.graphMatches(),item.profileScore(),item.profileMatches(),repeat,diversity,item.score()-repeat-diversity);
            }).sorted(Comparator.comparingInt(Recommendation::rerankScore).reversed().thenComparingLong(item->personalized?tieHash(signature,item.id()):item.id()).thenComparingLong(Recommendation::id)).toList();
            Recommendation chosen=scored.getFirst(); result.add(chosen);
            remaining.removeIf(item->item.id()==chosen.id());
            themes.merge(chosen.theme(),1,Integer::sum);regions.merge(chosen.region(),1,Integer::sum);
        }
        return result;
    }
    private long tieHash(String signature,long id) {
        try { byte[] hash=MessageDigest.getInstance("SHA-256").digest((signature+"|"+id).getBytes(StandardCharsets.UTF_8));return ByteBuffer.wrap(hash).getLong(); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
