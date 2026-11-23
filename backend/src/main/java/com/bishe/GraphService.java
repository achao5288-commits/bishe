package com.bishe;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.io.IOException;

@Service
public class GraphService implements AutoCloseable {
    public record Attraction(long id, String name, String region, String theme, String description, String image, String sourceName, String sourceUrl) {}
    record Event(String id, String name, String sourceUrl) {}
    record Link(long attractionId, String eventId, String sourceUrl) {}
    record Seed(List<Attraction> attractions, List<Event> events, List<Link> links) {}
    record Node(String id, String name, String label, String sourceUrl) {}
    record Edge(String source, String target, String type, String sourceUrl) {}
    record Graph(List<Node> nodes, List<Edge> edges) {}
    private final Driver driver;
    private final ObjectMapper mapper;
    GraphService(@Value("${NEO4J_URI:bolt://localhost:7687}") String uri,
                 @Value("${NEO4J_USER:neo4j}") String user,
                 @Value("${NEO4J_PASSWORD:local-dev-password}") String password, ObjectMapper mapper) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password)); this.mapper = mapper;
    }
    @PostConstruct void seed() throws IOException {
        Seed seed;
        try (var input = getClass().getResourceAsStream("/graph-seed.json")) { seed = mapper.readValue(input, Seed.class); }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT attraction_id IF NOT EXISTS FOR (a:Attraction) REQUIRE a.id IS UNIQUE").consume();
                tx.run("CREATE CONSTRAINT event_id IF NOT EXISTS FOR (e:Event) REQUIRE e.id IS UNIQUE").consume();
                tx.run("CREATE CONSTRAINT region_name IF NOT EXISTS FOR (r:Region) REQUIRE r.name IS UNIQUE").consume();
                return null;
            });
            session.executeWrite(tx -> {
                for (Attraction a : seed.attractions()) {
                    Map<String,Object> p = new HashMap<>(); p.put("id",a.id()); p.put("name",a.name()); p.put("region",a.region()); p.put("theme",a.theme()); p.put("description",a.description()); p.put("image",a.image()); p.put("sourceName",a.sourceName()); p.put("sourceUrl",a.sourceUrl());
                    tx.run("MERGE (a:Attraction {id:$id}) SET a.name=$name,a.region=$region,a.theme=$theme,a.description=$description,a.image=$image,a.sourceName=$sourceName,a.sourceUrl=$sourceUrl MERGE (r:Region {name:$region}) MERGE (a)-[rel:LOCATED_IN]->(r) SET rel.sourceUrl=$sourceUrl",p).consume();
                }
                for (Event e : seed.events()) tx.run("MERGE (e:Event {id:$id}) SET e.name=$name,e.sourceUrl=$sourceUrl",Map.of("id",e.id(),"name",e.name(),"sourceUrl",e.sourceUrl())).consume();
                for (Link l : seed.links()) tx.run("MATCH (a:Attraction {id:$attractionId}),(e:Event {id:$eventId}) MERGE (a)-[r:RELATED_TO_EVENT]->(e) SET r.sourceUrl=$sourceUrl",Map.of("attractionId",l.attractionId(),"eventId",l.eventId(),"sourceUrl",l.sourceUrl())).consume();
                return null;
            });
        }
    }
    public List<Attraction> attractions(String region) {
        try (Session session=driver.session()) { return session.executeRead(tx -> tx.run("MATCH (a:Attraction) WHERE $region = '' OR a.region = $region RETURN a ORDER BY a.id",Map.of("region",region == null ? "" : region)).list(r -> {
            var a=r.get("a"); return new Attraction(a.get("id").asLong(),a.get("name").asString(),a.get("region").asString(),a.get("theme").asString(),a.get("description").asString(),a.get("image").asString(),a.get("sourceName").asString(),a.get("sourceUrl").asString());
        })); }
    }
    public Graph graph(long id) {
        try (Session session=driver.session()) { return session.executeRead(tx -> {
            var result=tx.run("MATCH (a:Attraction {id:$id}) OPTIONAL MATCH (a)-[r:LOCATED_IN|RELATED_TO_EVENT]->(n) RETURN a,r,n ORDER BY type(r), n.name",Map.of("id",id)).list();
            if(result.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"景点不存在");
            var a=result.get(0).get("a"); String aid="attraction-"+id;
            List<Node> nodes=new ArrayList<>(); List<Edge> edges=new ArrayList<>();
            nodes.add(new Node(aid,a.get("name").asString(),"景点",a.get("sourceUrl").asString()));
            for(var row:result) { if(row.get("n").isNull()) continue; var n=row.get("n"); var rel=row.get("r").asRelationship(); boolean event=rel.type().equals("RELATED_TO_EVENT"); String nid=event ? n.get("id").asString() : "region-"+n.get("name").asString(); String source=rel.get("sourceUrl").asString(); nodes.add(new Node(nid,n.get("name").asString(),event?"历史事件":"地区",source)); edges.add(new Edge(aid,nid,rel.type(),source)); }
            return new Graph(nodes,edges);
        }); }
    }
    @Override public void close() { driver.close(); }
}

