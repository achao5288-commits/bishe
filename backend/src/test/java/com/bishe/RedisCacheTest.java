package com.bishe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class RedisCacheTest {
 @SuppressWarnings("unchecked") @Test void hitAndInvalidationReturnFreshValues() {
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  Map<String,String> values=new HashMap<>();when(ops.get(anyString())).thenAnswer(i->values.get(i.getArgument(0)));doAnswer(i->{values.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var calls=new AtomicInteger();var type=new TypeReference<List<String>>(){};
  assertEquals(List.of("1"),cache.get("attractions","延安",type,Duration.ofMinutes(5),()->List.of(""+calls.incrementAndGet())));
  assertEquals(List.of("1"),cache.get("attractions","延安",type,Duration.ofMinutes(5),()->List.of(""+calls.incrementAndGet())));
  cache.invalidate();assertEquals(List.of("2"),cache.get("attractions","延安",type,Duration.ofMinutes(5),()->List.of(""+calls.incrementAndGet())));
  verify(ops,times(2)).set(anyString(),anyString(),eq(Duration.ofMinutes(5)));
 }
 @SuppressWarnings("unchecked") @Test void failuresFallBackAndDistinctRequestsDoNotShare() {
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  Map<String,String> values=new HashMap<>();when(ops.get(anyString())).thenAnswer(i->values.get(i.getArgument(0)));doAnswer(i->{values.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var type=new TypeReference<String>(){};
  assertEquals("A",cache.get("recommendation",Map.of("history",List.of(1)),type,Duration.ofSeconds(60),()->"A"));
  assertEquals("B",cache.get("recommendation",Map.of("history",List.of(2)),type,Duration.ofSeconds(60),()->"B"));
  when(ops.get(anyString())).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
  assertEquals("fallback",cache.get("graph",1,type,Duration.ofSeconds(60),()->"fallback"));
 }
 @Test void loaderErrorsAreNotCached(){var redis=mock(StringRedisTemplate.class);var cache=new RedisCache(redis,new ObjectMapper(),false);assertThrows(IllegalArgumentException.class,()->cache.get("g",1,new TypeReference<String>(){},Duration.ofSeconds(1),()->{throw new IllegalArgumentException();}));}
 @SuppressWarnings("unchecked") @Test void inFlightWriteAfterInvalidationCannotRepopulateCurrentGeneration(){
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  Map<String,String> values=new HashMap<>();when(ops.get(anyString())).thenAnswer(i->values.get(i.getArgument(0)));doAnswer(i->{values.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var type=new TypeReference<String>(){};
  assertEquals("old",cache.get("graph",1,type,Duration.ofMinutes(1),()->{cache.invalidate();return "old";}));
  assertEquals("new",cache.get("graph",1,type,Duration.ofMinutes(1),()->"new"));
  assertEquals("new",cache.get("graph",1,type,Duration.ofMinutes(1),()->"unexpected"));
 }
 @Test void disabledCacheDoesNotContactRedis(){var redis=mock(StringRedisTemplate.class);var cache=new RedisCache(redis,new ObjectMapper(),false);assertEquals("source",cache.get("x",1,new TypeReference<String>(){},Duration.ofSeconds(1),()->"source"));verifyNoInteractions(redis);}
 @SuppressWarnings("unchecked") @Test void concurrentHotKeyLoadsOnlyOncePerProcess() throws Exception {
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);
  var values=new java.util.concurrent.ConcurrentHashMap<String,String>();when(ops.get(anyString())).thenAnswer(i->values.get(i.getArgument(0)));doAnswer(i->{values.put(i.getArgument(0),i.getArgument(1));return null;}).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var type=new TypeReference<String>(){};var calls=new AtomicInteger();
  var pool=java.util.concurrent.Executors.newFixedThreadPool(8);var gate=new java.util.concurrent.CountDownLatch(1);var tasks=new ArrayList<java.util.concurrent.Future<String>>();
  try{for(int i=0;i<8;i++)tasks.add(pool.submit(()->{gate.await();return cache.get("popular",1,type,Duration.ofMinutes(1),()->{calls.incrementAndGet();try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return "value";});}));gate.countDown();for(var task:tasks)assertEquals("value",task.get());assertEquals(1,calls.get());}finally{pool.shutdownNow();}
 }
 @SuppressWarnings("unchecked") @Test void writeFailureReturnsSourceAndTemporarilyBypassesRedis(){
  var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(ops);when(ops.get(anyString())).thenReturn(null);doThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline")).when(ops).set(anyString(),anyString(),any(Duration.class));
  var cache=new RedisCache(redis,new ObjectMapper(),true);var count=new AtomicInteger();var type=new TypeReference<String>(){};
  assertEquals("1",cache.get("graph",1,type,Duration.ofSeconds(10),()->""+count.incrementAndGet()));
  assertEquals("2",cache.get("graph",1,type,Duration.ofSeconds(10),()->""+count.incrementAndGet()));
  verify(ops,times(1)).set(anyString(),anyString(),any(Duration.class));
 }
}
