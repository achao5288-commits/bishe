package com.bishe;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
@Configuration
class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecretKey jwtKey(@Value("${APP_JWT_SECRET:}") String secret){byte[] bytes;if(secret.isBlank()){bytes=new byte[32];new SecureRandom().nextBytes(bytes);}else{bytes=secret.getBytes(StandardCharsets.UTF_8);if(bytes.length<32)throw new IllegalStateException("APP_JWT_SECRET must be at least 32 bytes");}return new SecretKeySpec(bytes,"HmacSHA256");}
 @Bean JwtEncoder jwtEncoder(SecretKey key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
 @Bean JwtDecoder jwtDecoder(SecretKey key){return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();}
 @Bean SecurityFilterChain security(HttpSecurity http,org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
  var converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(jwt->{var roles=jdbc.queryForList("SELECT role FROM app_users WHERE id=?",String.class,jwt.getSubject());if(roles.isEmpty())throw new org.springframework.security.authentication.BadCredentialsException("账户不可用");return java.util.List.of(new SimpleGrantedAuthority("ROLE_"+roles.getFirst()));});
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/attractions","/api/attractions/**","/api/graph","/api/graph/**").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/recommendations","/api/chat").permitAll()
    .requestMatchers("/api/evaluation/summary","/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/me/**","/api/evaluation/**").authenticated().anyRequest().denyAll())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
 }
}
