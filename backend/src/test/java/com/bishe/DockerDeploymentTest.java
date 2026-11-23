package com.bishe;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import static org.junit.jupiter.api.Assertions.*;
class DockerDeploymentTest {
 @SuppressWarnings("unchecked") @Test void databaseDependenciesArePrivateAndBackendWaitsForReadiness() throws Exception {
  Path root=Path.of("..");var compose=(Map<String,Object>)new Yaml().load(Files.readString(root.resolve("compose.yaml")));var services=(Map<String,Object>)compose.get("services");
  assertTrue(services.keySet().containsAll(Set.of("frontend","backend","mysql","neo4j","redis")));
  var mysql=(Map<String,Object>)services.get("mysql");var neo4j=(Map<String,Object>)services.get("neo4j");
  assertFalse(mysql.containsKey("ports"));assertFalse(neo4j.containsKey("ports"));assertTrue(mysql.containsKey("healthcheck"));assertTrue(neo4j.containsKey("healthcheck"));
  var backend=(Map<String,Object>)services.get("backend");var dependencies=(Map<String,Object>)backend.get("depends_on");
  assertEquals("service_healthy",((Map<String,Object>)dependencies.get("mysql")).get("condition"));assertEquals("service_healthy",((Map<String,Object>)dependencies.get("neo4j")).get("condition"));
  assertTrue(Files.exists(root.resolve("backend/Dockerfile")));assertTrue(Files.exists(root.resolve("frontend/Dockerfile")));assertTrue(Files.exists(root.resolve(".env.example")));
 }
}
