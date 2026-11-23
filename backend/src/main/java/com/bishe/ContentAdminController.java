package com.bishe;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.*;
import org.neo4j.driver.Session;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/content")
class ContentAdminController {
    record Entity(String id, String name, String kind, String sourceUrl) {}
    record Relation(String source, String target, String type, String sourceUrl) {}
    private static final Set<String> KINDS=Set.of("Event","Person","Work","Theme");
    private static final Set<String> TYPES=Set.of("RELATED_TO_EVENT","RELATED_TO_PERSON","RELATED_TO_WORK","BELONGS_TO_THEME","PARTICIPATED_IN");
    private final GraphService graph; private final StoryService stories; private final RedisCache cache; private final ObjectMapper mapper;
    ContentAdminController(GraphService graph,StoryService stories,RedisCache cache,ObjectMapper mapper){this.graph=graph;this.stories=stories;this.cache=cache;this.mapper=mapper;}
    private static void require(boolean ok,String message){if(!ok)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static boolean text(String s,int max){return s!=null&&!s.isBlank()&&s.length()<=max;}
    private static boolean url(String value){try {URI u=URI.create(value);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())) && u.getHost()!=null && u.getUserInfo()==null && value.length()<=2000;}catch(Exception e){return false;}}
    private static boolean identifier(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{1,100}");}
    private static void found(long n){if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"资料不存在");}
    private void changed(){cache.invalidate();}
    @GetMapping("/attractions") List<GraphService.Attraction> attractions(){return graph.attractions("");}
    @GetMapping("/stories/{id}") StoryService.Story story(@PathVariable long id){return stories.find(id);}
    @PutMapping("/attractions/{id}") GraphService.Attraction putAttraction(@PathVariable long id,@RequestBody GraphService.Attraction a){
        require(id>0&&id==a.id(),"景点编号不匹配");
        require(text(a.name(),150)&&text(a.region(),100)&&text(a.theme(),100)&&text(a.description(),4000)&&text(a.sourceName(),200)&&url(a.sourceUrl()),"景点资料或来源无效");
        require(text(a.image(),2000)&&(a.image().matches("/images/[A-Za-z0-9_./-]+")&&!a.image().contains("..")||url(a.image())),"插画路径无效");
        require(a.aliases()!=null&&a.aliases().size()<=20&&a.aliases().stream().allMatch(x->text(x,100)),"别名无效");
        Map<String,Object> p=new HashMap<>();p.put("id",id);p.put("name",a.name());p.put("region",a.region());p.put("theme",a.theme());p.put("description",a.description());p.put("image",a.image());p.put("sourceName",a.sourceName());p.put("sourceUrl",a.sourceUrl());p.put("aliases",a.aliases());
        try(Session s=graph.driver().session()){s.executeWrite(tx->{tx.run("MERGE (a:Attraction {id:$id}) SET a.name=$name,a.region=$region,a.theme=$theme,a.description=$description,a.image=$image,a.sourceName=$sourceName,a.sourceUrl=$sourceUrl,a.aliases=$aliases WITH a OPTIONAL MATCH (a)-[old:LOCATED_IN]->() DELETE old WITH DISTINCT a MERGE (r:Region {name:$region}) MERGE (a)-[rel:LOCATED_IN]->(r) SET rel.sourceUrl=$sourceUrl",p).consume();return null;});}changed();return a;
    }
    @DeleteMapping("/attractions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteAttraction(@PathVariable long id){try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (a:Attraction {id:$id}) WITH collect(a) AS nodes FOREACH (a IN nodes | DETACH DELETE a) RETURN size(nodes) AS n",Map.of("id",id)).single().get("n").asLong());found(n);}changed();}
    @PutMapping("/stories/{id}") StoryService.Story putStory(@PathVariable long id,@RequestBody StoryService.Story story) throws Exception {
        require(id>0&&id==story.attractionId()&&text(story.title(),200),"故事编号或标题无效");
        require(story.sections()!=null&&!story.sections().isEmpty()&&story.sections().size()<=30,"故事必须包含 1 至 30 个章节");
        for(var section:story.sections()){require(section!=null&&text(section.title(),200)&&text(section.content(),10000)&&section.sources()!=null&&!section.sources().isEmpty()&&section.sources().size()<=20,"章节及来源不能为空");for(var source:section.sources())require(source!=null&&text(source.name(),200)&&url(source.url()),"来源名称或链接无效");}
        String json=mapper.writeValueAsString(story);try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (a:Attraction {id:$id}) SET a.storyJson=$json RETURN count(a) AS n",Map.of("id",id,"json",json)).single().get("n").asLong());found(n);}changed();return story;
    }
    @DeleteMapping("/stories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteStory(@PathVariable long id){try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (a:Attraction {id:$id}) WHERE a.storyJson IS NOT NULL REMOVE a.storyJson RETURN count(a) AS n",Map.of("id",id)).single().get("n").asLong());found(n);}changed();}
    @GetMapping("/entities") List<Entity> entities(){try(Session s=graph.driver().session()){return s.executeRead(tx->tx.run("MATCH (n) WHERE n:Event OR n:Knowledge RETURN n ORDER BY n.name").list(row->{var n=row.get("n");String kind=KINDS.stream().filter(k->n.asNode().hasLabel(k)).findFirst().orElse("Event");return new Entity(n.get("id").asString(),n.get("name").asString(),kind,n.get("sourceUrl").asString(""));}));}}
    @PutMapping("/entities/{id}") Entity putEntity(@PathVariable String id,@RequestBody Entity e){require(identifier(id)&&!id.startsWith("attraction-")&&!id.startsWith("region-")&&id.equals(e.id())&&text(e.name(),150)&&KINDS.contains(e.kind())&&url(e.sourceUrl()),"实体资料无效");
        try(Session s=graph.driver().session()){s.executeWrite(tx->{long existing=tx.run("MATCH (n) WHERE (n:Event OR n:Knowledge) AND n.id=$id RETURN count(n) AS n",Map.of("id",id)).single().get("n").asLong();if(existing>0){long same=tx.run("MATCH (n:"+e.kind()+" {id:$id}) RETURN count(n) AS n",Map.of("id",id)).single().get("n").asLong();if(same!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"实体类型不可修改");}String labels=e.kind().equals("Event")?"Event":"Knowledge:"+e.kind();tx.run("MERGE (n:"+labels+" {id:$id}) SET n.name=$name,n.sourceUrl=$url",Map.of("id",id,"name",e.name(),"url",e.sourceUrl())).consume();return null;});}changed();return e;}
    @DeleteMapping("/entities/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteEntity(@PathVariable String id){require(identifier(id),"实体编号无效");try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (a) WHERE (a:Event OR a:Knowledge) AND a.id=$id WITH collect(a) AS nodes FOREACH (a IN nodes | DETACH DELETE a) RETURN size(nodes) AS n",Map.of("id",id)).single().get("n").asLong());found(n);}changed();}
    @GetMapping("/relations") List<Relation> relations(){try(Session s=graph.driver().session()){return s.executeRead(tx->tx.run("MATCH (s)-[r:RELATED_TO_EVENT|RELATED_TO_PERSON|RELATED_TO_WORK|BELONGS_TO_THEME|PARTICIPATED_IN]->(t) RETURN CASE WHEN s:Attraction THEN 'attraction-'+toString(s.id) ELSE s.id END AS source, CASE WHEN t:Attraction THEN 'attraction-'+toString(t.id) ELSE t.id END AS target, type(r) AS type, coalesce(r.sourceUrl,'') AS url ORDER BY source,target,type").list(row->new Relation(row.get("source").asString(),row.get("target").asString(),row.get("type").asString(),row.get("url").asString())));}}
    private void validateRelation(Relation r){require(r!=null&&identifier(r.source())&&identifier(r.target())&&!r.source().equals(r.target())&&TYPES.contains(r.type())&&url(r.sourceUrl()),"关系或来源无效");}
    @PutMapping("/relations") Relation putRelation(@RequestBody Relation r){validateRelation(r);try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (s),(t) WHERE (CASE WHEN s:Attraction THEN 'attraction-'+toString(s.id) ELSE s.id END)=$source AND (CASE WHEN t:Attraction THEN 'attraction-'+toString(t.id) ELSE t.id END)=$target AND (s:Attraction OR s:Event OR s:Knowledge) AND (t:Event OR t:Knowledge) AND (($type='RELATED_TO_EVENT' AND s:Attraction AND t:Event) OR ($type='RELATED_TO_PERSON' AND s:Attraction AND t:Person) OR ($type='BELONGS_TO_THEME' AND s:Attraction AND t:Theme) OR ($type='RELATED_TO_WORK' AND (s:Attraction OR s:Event) AND t:Work) OR ($type='PARTICIPATED_IN' AND (s:Attraction OR s:Person) AND t:Event)) MERGE (s)-[r:"+r.type()+"]->(t) SET r.sourceUrl=$url RETURN count(r) AS n",Map.of("source",r.source(),"target",r.target(),"type",r.type(),"url",r.sourceUrl())).single().get("n").asLong());found(n);}changed();return r;}
    @DeleteMapping("/relations") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteRelation(@RequestBody Relation r){validateRelation(r);try(Session s=graph.driver().session()){long n=s.executeWrite(tx->tx.run("MATCH (s)-[r:"+r.type()+"]->(t) WHERE (CASE WHEN s:Attraction THEN 'attraction-'+toString(s.id) ELSE s.id END)=$source AND (CASE WHEN t:Attraction THEN 'attraction-'+toString(t.id) ELSE t.id END)=$target WITH collect(r) AS rels FOREACH (r IN rels | DELETE r) RETURN size(rels) AS n",Map.of("source",r.source(),"target",r.target())).single().get("n").asLong());found(n);}changed();}
}
