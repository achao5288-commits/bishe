package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class RecommendationCacheTest {
 @SuppressWarnings("unchecked") @Test void requestsUseSeparateEntriesAndInvalidationRecalculates() {
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  Map<String,String> values=new HashMap<>();when(ops.get(anyString())).thenAnswer(i->values.get(i.getArgument(0)));doAnswer(i->{values.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var graph=mock(GraphService.class);var stories=mock(StoryService.class);
  when(graph.attractions("延安")).thenReturn(List.of(new GraphService.Attraction(1,"景点","延安","革命旧址","","","","",List.of())));
  when(graph.matchInterests(eq("延安"),anyList())).thenReturn(Map.of());
  var controller=new AttractionController(graph,stories,cache);
  var a=new AttractionController.Request("延安",List.of(),List.of(),Map.of(),List.of(),List.of(),5);
  var b=new AttractionController.Request("延安",List.of(),List.of(),Map.of(),List.of(1L),List.of(),5);
  assertEquals(0,controller.recommendations(a).getFirst().repeatPenalty());
  assertEquals(0,controller.recommendations(a).getFirst().repeatPenalty());
  assertEquals(50,controller.recommendations(b).getFirst().repeatPenalty());
  verify(graph,times(2)).attractions("延安");
  assertTrue(values.keySet().stream().allMatch(k->!k.contains("延安")&&!k.contains("history")));
  cache.invalidate();controller.recommendations(a);verify(graph,times(3)).attractions("延安");
 }
}
