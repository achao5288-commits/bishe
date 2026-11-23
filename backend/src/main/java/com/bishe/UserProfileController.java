package com.bishe;
import java.security.Principal;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/me")
class UserProfileController {
 record Profile(List<Long> favorites,List<Long> history){}
 record AccountView(String id,String username,String role){}
 private final JdbcTemplate jdbc; private final GraphService graph;
 UserProfileController(JdbcTemplate jdbc,GraphService graph){this.jdbc=jdbc;this.graph=graph;}
 @GetMapping("/account") AccountView account(@AuthenticationPrincipal Jwt jwt,org.springframework.security.core.Authentication auth){String role=auth.getAuthorities().stream().map(a->a.getAuthority().replaceFirst("^ROLE_", "")).findFirst().orElse("USER");return new AccountView(jwt.getSubject(),jwt.getClaimAsString("username"),role);}
 @GetMapping("/profile") Profile profile(Principal principal){String id=principal.getName();return new Profile(jdbc.queryForList("SELECT attraction_id FROM user_favorites WHERE user_id=? ORDER BY created_at DESC,attraction_id",Long.class,id),jdbc.queryForList("SELECT attraction_id FROM user_history WHERE user_id=? ORDER BY viewed_at DESC,attraction_id",Long.class,id));}
 private void validate(long attractionId){if(graph.attractions(null).stream().noneMatch(a->a.id()==attractionId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"景点不存在");}
 @PutMapping("/favorites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional void favorite(Principal principal,@PathVariable long id){validate(id);String user=principal.getName();if(jdbc.queryForObject("SELECT COUNT(*) FROM user_favorites WHERE user_id=?",Integer.class,user)>=100 && jdbc.queryForObject("SELECT COUNT(*) FROM user_favorites WHERE user_id=? AND attraction_id=?",Integer.class,user,id)==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"收藏已满");try{jdbc.update("INSERT INTO user_favorites(user_id,attraction_id,created_at) VALUES(?,?,?)",user,id,System.currentTimeMillis());}catch(DuplicateKeyException ignored){}}
 @DeleteMapping("/favorites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void unfavorite(Principal principal,@PathVariable long id){jdbc.update("DELETE FROM user_favorites WHERE user_id=? AND attraction_id=?",principal.getName(),id);}
 @PostMapping("/history/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional void view(Principal principal,@PathVariable long id){validate(id);String user=principal.getName();long now=System.currentTimeMillis();if(jdbc.update("UPDATE user_history SET viewed_at=? WHERE user_id=? AND attraction_id=?",now,user,id)==0){try{jdbc.update("INSERT INTO user_history(user_id,attraction_id,viewed_at) VALUES(?,?,?)",user,id,now);}catch(DuplicateKeyException e){jdbc.update("UPDATE user_history SET viewed_at=? WHERE user_id=? AND attraction_id=?",now,user,id);}}List<Long> rows=jdbc.queryForList("SELECT attraction_id FROM user_history WHERE user_id=? ORDER BY viewed_at DESC,attraction_id",Long.class,user);for(Long old:rows.stream().skip(50).toList())jdbc.update("DELETE FROM user_history WHERE user_id=? AND attraction_id=?",user,old);}
 @DeleteMapping("/history") @ResponseStatus(HttpStatus.NO_CONTENT) void clearHistory(Principal principal){jdbc.update("DELETE FROM user_history WHERE user_id=?",principal.getName());}
 @DeleteMapping("/favorites") @ResponseStatus(HttpStatus.NO_CONTENT) void clearFavorites(Principal principal){jdbc.update("DELETE FROM user_favorites WHERE user_id=?",principal.getName());}
}
