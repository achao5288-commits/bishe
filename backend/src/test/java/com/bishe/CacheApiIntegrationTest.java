package com.bishe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties="app.cache.enabled=true") @AutoConfigureMockMvc
class CacheApiIntegrationTest {
 @Autowired MockMvc mvc; @MockitoBean StringRedisTemplate redis;
 @SuppressWarnings("unchecked") @BeforeEach void setup(){ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);var data=new ConcurrentHashMap<String,String>();when(ops.get(anyString())).thenAnswer(i->data.get(i.getArgument(0)));doAnswer(i->{data.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));}
 @Test @WithMockUser(roles="ADMIN") void graphAndAttractionsUseCacheAndAdminCanInvalidate() throws Exception {
  mvc.perform(get("/api/attractions").param("region","延安")).andExpect(status().isOk());mvc.perform(get("/api/attractions").param("region","延安")).andExpect(status().isOk());
  mvc.perform(get("/api/graph/attractions/1")).andExpect(status().isOk());mvc.perform(get("/api/graph/attractions/1")).andExpect(status().isOk());
  verify(redis.opsForValue(),times(1)).set(contains(":attractions:"),anyString(),eq(Duration.ofMinutes(10)));
  verify(redis.opsForValue(),times(1)).set(contains(":attraction-graph:"),anyString(),eq(Duration.ofMinutes(10)));
  mvc.perform(post("/api/admin/cache/invalidate")).andExpect(status().isNoContent());
  mvc.perform(get("/api/attractions").param("region","延安")).andExpect(status().isOk());
  verify(redis.opsForValue(),times(2)).set(contains(":attractions:"),anyString(),eq(Duration.ofMinutes(10)));
 }
 @Test @WithMockUser(roles="USER") void ordinaryUserCannotInvalidate() throws Exception {mvc.perform(post("/api/admin/cache/invalidate")).andExpect(status().isForbidden());}
 @Test void missingGraphIsNeverCached() throws Exception {mvc.perform(get("/api/graph/attractions/999")).andExpect(status().isNotFound());mvc.perform(get("/api/graph/attractions/999")).andExpect(status().isNotFound());verify(redis.opsForValue(),never()).set(contains(":attraction-graph:"),anyString(),any(Duration.class));}
}
