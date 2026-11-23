package com.bishe;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="REDIS_INTEGRATION",matches="true")
class RealRedisIntegrationTest {
 @Test void realRedisRoundTripAndInvalidation(){
  String host=System.getenv().getOrDefault("REDIS_HOST","127.0.0.1");int port=Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT","6379"));
  var config=new RedisStandaloneConfiguration(host,port);String password=System.getenv("REDIS_PASSWORD");if(password!=null && !password.isBlank())config.setPassword(RedisPassword.of(password));
  var factory=new LettuceConnectionFactory(config);factory.afterPropertiesSet();factory.start();
  try{var template=new StringRedisTemplate(factory);template.afterPropertiesSet();var cache=new RedisCache(template,new ObjectMapper(),true);var count=new AtomicInteger();var type=new TypeReference<String>(){};
   assertEquals("1",cache.get("integration-test",1,type,Duration.ofSeconds(5),()->""+count.incrementAndGet()));
   assertEquals("1",cache.get("integration-test",1,type,Duration.ofSeconds(5),()->""+count.incrementAndGet()));
   cache.invalidate();assertEquals("2",cache.get("integration-test",1,type,Duration.ofSeconds(5),()->""+count.incrementAndGet()));
  }finally{factory.destroy();}
 }
}
