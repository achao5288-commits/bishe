package com.bishe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
@Service
class RedisCache {
 private static final Logger log=LoggerFactory.getLogger(RedisCache.class);
 private final StringRedisTemplate redis;private final ObjectMapper mapper;private final boolean enabled;
 @Value("${app.cache.attraction-ttl:PT10M}") private Duration attractionTtl=Duration.ofMinutes(10);
 @Value("${app.cache.graph-ttl:PT10M}") private Duration graphTtl=Duration.ofMinutes(10);
 @Value("${app.cache.recommendation-ttl:PT1M}") private Duration recommendationTtl=Duration.ofMinutes(1);
 Duration attractionTtl(){return attractionTtl;}
 Duration graphTtl(){return graphTtl;}
 Duration recommendationTtl(){return recommendationTtl;}
 private final AtomicReference<String> generation=new AtomicReference<>(UUID.randomUUID().toString());
 private final AtomicLong retryAfter=new AtomicLong(0);
 private final Object[] locks=java.util.stream.IntStream.range(0,64).mapToObj(i->new Object()).toArray();
 RedisCache(StringRedisTemplate redis,ObjectMapper mapper,@Value("${app.cache.enabled:false}") boolean enabled){this.redis=redis;this.mapper=mapper;this.enabled=enabled;}
 private String key(String namespace,String kind,Object key) throws Exception {
  byte[] input=mapper.writeValueAsBytes(key);String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
  return "bishe:cache:v1:"+namespace+":"+kind+":"+digest;
 }
 private void unavailable(){long until=System.currentTimeMillis()+5000;long previous=retryAfter.getAndSet(until);if(previous<System.currentTimeMillis())log.warn("Redis cache unavailable; temporarily using database queries");}
 <T> T get(String kind,Object key,TypeReference<T> type,Duration ttl,Supplier<T> loader){
  if(!enabled || ttl.isZero() || ttl.isNegative() || System.currentTimeMillis()<retryAfter.get())return loader.get();
  String cacheKey;
  try{cacheKey=key(generation.get(),kind,key);String value=redis.opsForValue().get(cacheKey);if(value!=null)return mapper.readValue(value,type);}
  catch(Exception e){unavailable();return loader.get();}
  synchronized(locks[Math.floorMod(cacheKey.hashCode(),locks.length)]){
   if(System.currentTimeMillis()<retryAfter.get())return loader.get();
   try{String value=redis.opsForValue().get(cacheKey);if(value!=null)return mapper.readValue(value,type);}
   catch(Exception e){unavailable();return loader.get();}
   T result=loader.get();
   if(result!=null){try{redis.opsForValue().set(cacheKey,mapper.writeValueAsString(result),ttl);}catch(Exception e){unavailable();}}
   return result;
  }
 }
 void invalidate(){generation.set(UUID.randomUUID().toString());retryAfter.set(0);}
}
