package com.bishe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
class AccountService {
 record Account(String id,String username,String passwordHash,String role){}
 record AuthResult(String token,String userId,String username,String role){}
 private final JdbcTemplate jdbc; private final PasswordEncoder encoder; private final JwtEncoder jwt;
 private final String dummyHash;
 AccountService(JdbcTemplate jdbc,PasswordEncoder encoder,JwtEncoder jwt){this.jdbc=jdbc;this.encoder=encoder;this.jwt=jwt;this.dummyHash=encoder.encode("not-a-real-password");}
 void validatePassword(String password){if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"密码过长");}
 Account find(String username){List<Account> rows=jdbc.query("SELECT id,username,password_hash,role FROM app_users WHERE username=?",(r,n)->new Account(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),username);return rows.isEmpty()?null:rows.getFirst();}
 Account create(String username,String password,String role){validatePassword(password);var a=new Account(UUID.randomUUID().toString(),username,encoder.encode(password),role);try{jdbc.update("INSERT INTO app_users(id,username,password_hash,role,created_at) VALUES(?,?,?,?,?)",a.id(),a.username(),a.passwordHash(),a.role(),System.currentTimeMillis());}catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"用户名已存在");}return a;}
 AuthResult register(String username,String password){return token(create(username,password,"USER"));}
 AuthResult login(String username,String password){validatePassword(password);Account account=find(username);boolean match=encoder.matches(password,account==null?dummyHash:account.passwordHash());if(account==null||!match)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户名或密码错误");return token(account);}
 AuthResult token(Account a){Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().subject(a.id()).claim("username",a.username()).claim("role",a.role()).issuedAt(now).expiresAt(now.plusSeconds(8*60*60)).build();String token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();return new AuthResult(token,a.id(),a.username(),a.role());}
}
