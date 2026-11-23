package com.bishe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.io.IOException;

@Service
public class GraphService implements AutoCloseable {
    public record Attraction(long id, String name, String region, String theme, String description, String image, String sourceName, String sourceUrl, List<String> aliases) {}
    record Event(String id, String name, String sourceUrl) {}
    record Link(long attractionId, String eventId, String sourceUrl) {}
    record Entity(String id, String name, String kind, String sourceUrl) {}
    record Relation(String start, String end, String type, String sourceUrl) {}
    record Seed(List<Attraction> attractions, List<Event> events, List<Link> links, List<Entity> entities, List<Relation> relations) {}
    record GraphNode(String id, String name, String label, String sourceUrl) {}
    record Edge(String source, String target, String type, String sourceUrl) {}
    record Graph(List<GraphNode> nodes, List<Edge> edges) {}
    public record Match(String id, String name, int distance, List<String> path, List<String> sources, List<String> relationTypes, int points) {}
    private static final Map<String,Integer> RELATION_WEIGHTS=Map.of("RELATED_TO_EVENT",60,"RELATED_TO_PERSON",50,"RELATED_TO_WORK",45,"BELONGS_TO_THEME",40,"PARTICIPATED_IN",45);
    private static int pathPoints(List<String> types) { int base=types.stream().mapToInt(t->RELATION_WEIGHTS.getOrDefault(t,0)).min().orElse(0); return types.size()==1?base:(base+1)/2; }
    private static final Map<String,String> LABELS=Map.of("Person","人物","Event","历史事件","Work","著作","Theme","主题","Region","地区","Attraction","景点");
    private static final Set<String> ENTITY_KINDS=Set.of("Person","Work","Theme");
    private static final Set<String> RELATION_TYPES=Set.of("RELATED_TO_PERSON","PARTICIPATED_IN","RELATED_TO_WORK","BELONGS_TO_THEME");
    private final Driver driver;
    private final ObjectMapper mapper;
    private final RedisCache cache;
    GraphService(@Value("${NEO4J_URI:bolt://localhost:7687}") String uri, @Value("${NEO4J_USER:neo4j}") String user, @Value("${NEO4J_PASSWORD:local-dev-password}") String password, ObjectMapper mapper, RedisCache cache) {
        this.driver=GraphDatabase.driver(uri,AuthTokens.basic(user,password)); this.mapper=mapper; this.cache=cache;
    }
    @PostConstruct void seed() throws IOException {
        Seed seed;
        List<StoryService.Story> seedStories;
        try(var input=getClass().getResourceAsStream("/attraction-stories.json")){ seedStories=mapper.readValue(input,new TypeReference<List<StoryService.Story>>(){}); }
        try(var input=getClass().getResourceAsStream("/graph-seed.json")){ seed=mapper.readValue(input,Seed.class); }
        try(Session session=driver.session()) {
            session.executeWrite(tx->{
                tx.run("CREATE CONSTRAINT attraction_id IF NOT EXISTS FOR (a:Attraction) REQUIRE a.id IS UNIQUE").consume();
                tx.run("CREATE CONSTRAINT event_id IF NOT EXISTS FOR (e:Event) REQUIRE e.id IS UNIQUE").consume();
                tx.run("CREATE CONSTRAINT region_name IF NOT EXISTS FOR (r:Region) REQUIRE r.name IS UNIQUE").consume();
                tx.run("CREATE CONSTRAINT knowledge_id IF NOT EXISTS FOR (k:Knowledge) REQUIRE k.id IS UNIQUE").consume();
                return null;
            });
            session.executeWrite(tx->{
                if(tx.run("MATCH (s:SeedControl {key:$key}) RETURN count(s) AS n",Map.of("key","initial-content-v1")).single().get("n").asLong()>0) return null;
                for(Attraction a:seed.attractions()) {
                    Map<String,Object> p=new HashMap<>(); p.put("id",a.id());p.put("name",a.name());p.put("region",a.region());p.put("theme",a.theme());p.put("description",a.description());p.put("image",a.image());p.put("sourceName",a.sourceName());p.put("sourceUrl",a.sourceUrl());p.put("aliases",a.aliases()==null?List.of():a.aliases());
                    tx.run("MERGE (a:Attraction {id:$id}) ON CREATE SET a.name=$name,a.region=$region,a.theme=$theme,a.description=$description,a.image=$image,a.sourceName=$sourceName,a.sourceUrl=$sourceUrl,a.aliases=$aliases MERGE (r:Region {name:$region}) MERGE (a)-[rel:LOCATED_IN]->(r) ON CREATE SET rel.sourceUrl=$sourceUrl",p).consume();
                }
                for(Event e:seed.events()) tx.run("MERGE (e:Event {id:$id}) ON CREATE SET e.name=$name,e.sourceUrl=$sourceUrl",Map.of("id",e.id(),"name",e.name(),"sourceUrl",e.sourceUrl())).consume();
                for(Link l:seed.links()) tx.run("MATCH (a:Attraction {id:$attractionId}),(e:Event {id:$eventId}) MERGE (a)-[r:RELATED_TO_EVENT]->(e) ON CREATE SET r.sourceUrl=$sourceUrl",Map.of("attractionId",l.attractionId(),"eventId",l.eventId(),"sourceUrl",l.sourceUrl())).consume();
                for(Entity e:seed.entities()) {
                    if(!ENTITY_KINDS.contains(e.kind())) throw new IllegalArgumentException("Unsupported entity kind: "+e.kind());
                    tx.run("MERGE (n:Knowledge {id:$id}) ON CREATE SET n:"+e.kind()+", n.name=$name,n.sourceUrl=$sourceUrl",Map.of("id",e.id(),"name",e.name(),"sourceUrl",e.sourceUrl())).consume();
                }
                for(Relation r:seed.relations()) {
                    if(!RELATION_TYPES.contains(r.type())) throw new IllegalArgumentException("Unsupported relation: "+r.type());
                    String cypher="MATCH (s),(t) WHERE (CASE WHEN s:Attraction THEN 'attraction-'+toString(s.id) ELSE s.id END)=$start AND (CASE WHEN t:Attraction THEN 'attraction-'+toString(t.id) ELSE t.id END)=$end MERGE (s)-[r:"+r.type()+"]->(t) ON CREATE SET r.sourceUrl=$sourceUrl RETURN count(r) AS count";
                    long count=tx.run(cypher,Map.of("start",r.start(),"end",r.end(),"sourceUrl",r.sourceUrl())).single().get("count").asLong();
                    if(count!=1) throw new IllegalStateException("Relation endpoints not unique or missing: "+r);
                }
                for(StoryService.Story story:seedStories) {
                    try { tx.run("MATCH (a:Attraction {id:$id}) WHERE a.storyJson IS NULL SET a.storyJson=$story",Map.of("id",story.attractionId(),"story",mapper.writeValueAsString(story))).consume(); }
                    catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException(e); }
                }
                tx.run("CREATE (:SeedControl {key:$key})",Map.of("key","initial-content-v1")).consume();
                return null;
            });
        }
    }
    Driver driver(){return driver;}
    public List<Attraction> attractions(String region) {
        return cache.get("attractions",region==null?"":region,new TypeReference<List<Attraction>>() {},cache.attractionTtl(),()->loadAttractions(region));
    }
    private List<Attraction> loadAttractions(String region) {
        try(Session session=driver.session()){return session.executeRead(tx->tx.run("MATCH (a:Attraction) WHERE $region = '' OR a.region = $region RETURN a ORDER BY a.id",Map.of("region",region==null?"":region)).list(r->{var a=r.get("a");return new Attraction(a.get("id").asLong(),a.get("name").asString(),a.get("region").asString(),a.get("theme").asString(),a.get("description").asString(),a.get("image").asString(),a.get("sourceName").asString(),a.get("sourceUrl").asString(),a.get("aliases").isNull()?List.of():a.get("aliases").asList(v->v.asString()));}));}
    }
    public Map<Long,List<Match>> matchInterests(String region, List<String> interests) {
        if (interests.isEmpty()) return Map.of();
        String query="MATCH p=(a:Attraction)-[:RELATED_TO_EVENT|RELATED_TO_PERSON|BELONGS_TO_THEME|PARTICIPATED_IN|RELATED_TO_WORK*1..2]->(n) "
            + "WHERE ($region = '' OR a.region = $region) AND (n:Person OR n:Event OR n:Work OR n:Theme) AND n.id IN $interests "
            + "RETURN a.id AS attractionId,n.id AS interestId,n.name AS name,length(p) AS distance, "
            + "[node IN nodes(p) | node.name] AS path,[rel IN relationships(p) | coalesce(rel.sourceUrl,'')] AS sources,[rel IN relationships(p) | type(rel)] AS relationTypes "
            + "ORDER BY a.id,n.id,length(p),[node IN nodes(p) | node.name]";
        try (Session session=driver.session()) {
            return session.executeRead(tx -> {
                Map<Long,LinkedHashMap<String,Match>> byAttraction=new LinkedHashMap<>();
                for (var row:tx.run(query,Map.of("region",region==null?"":region,"interests",interests)).list()) {
                    long attractionId=row.get("attractionId").asLong(); String interestId=row.get("interestId").asString();
                    List<String> types=row.get("relationTypes").asList(v->v.asString());
                    Match match=new Match(interestId,row.get("name").asString(),row.get("distance").asInt(),row.get("path").asList(v->v.asString()),row.get("sources").asList(v->v.asString()),types,pathPoints(types));
                    byAttraction.computeIfAbsent(attractionId,k->new LinkedHashMap<>()).merge(interestId,match,(oldMatch,newMatch)->newMatch.points()>oldMatch.points()?newMatch:oldMatch);
                }
                Map<Long,List<Match>> result=new HashMap<>();
                byAttraction.forEach((key,value)->result.put(key,List.copyOf(value.values())));
                return result;
            });
        }
    }
    private String id(Node n){return n.hasLabel("Attraction")?"attraction-"+n.get("id").asLong():n.hasLabel("Region")?"region-"+n.get("name").asString():n.get("id").asString();}
    private GraphNode node(Node n,String fallback){String label=LABELS.entrySet().stream().filter(e->n.hasLabel(e.getKey())).map(Map.Entry::getValue).findFirst().orElse("实体");return new GraphNode(id(n),n.get("name").asString(),label,n.get("sourceUrl").asString(fallback));}
    private Graph assemble(List<Record> rows){
        Map<String,GraphNode> nodes=new LinkedHashMap<>();Map<String,Edge> edges=new LinkedHashMap<>();
        for(Record row:rows){Node s=row.get("s").asNode(),t=row.get("t").asNode();Relationship r=row.get("r").asRelationship();String source=r.get("sourceUrl").asString("");GraphNode from=node(s,source),to=node(t,source);nodes.putIfAbsent(from.id(),from);nodes.putIfAbsent(to.id(),to);Edge edge=new Edge(from.id(),to.id(),r.type(),source);edges.putIfAbsent(from.id()+"|"+r.type()+"|"+to.id(),edge);}
        return new Graph(new ArrayList<>(nodes.values()),new ArrayList<>(edges.values()));
    }
    public Graph graph(long id){
        return cache.get("attraction-graph",id,new TypeReference<Graph>() {},cache.graphTtl(),()->loadGraph(id));
    }
    private Graph loadGraph(long id){
        try(Session session=driver.session()){return session.executeRead(tx->{
            if(tx.run("MATCH (a:Attraction {id:$id}) RETURN count(a) AS n",Map.of("id",id)).single().get("n").asLong()==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"景点不存在");
            return assemble(tx.run("MATCH p=(a:Attraction {id:$id})-[:LOCATED_IN|RELATED_TO_EVENT|RELATED_TO_PERSON|BELONGS_TO_THEME|PARTICIPATED_IN|RELATED_TO_WORK*1..2]->() UNWIND relationships(p) AS r RETURN DISTINCT startNode(r) AS s,endNode(r) AS t,r ORDER BY s.name,t.name",Map.of("id",id)).list());
        });}
    }
    public Graph allGraph(){return cache.get("full-graph","all",new TypeReference<Graph>() {},cache.graphTtl(),this::loadAllGraph);}
    private Graph loadAllGraph(){try(Session session=driver.session()){return session.executeRead(tx->assemble(tx.run("MATCH (s)-[r:LOCATED_IN|RELATED_TO_EVENT|RELATED_TO_PERSON|BELONGS_TO_THEME|PARTICIPATED_IN|RELATED_TO_WORK]->(t) RETURN s,t,r ORDER BY s.name,t.name LIMIT 500").list()));}}
    @Override public void close(){driver.close();}
}
