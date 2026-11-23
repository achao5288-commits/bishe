package com.bishe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/admin/users")
class AdminUsersController {
 record UserView(String id,String username,String role,boolean protectedUser){}
 record RoleRequest(@NotBlank @Pattern(regexp="USER|ADMIN") String role){}
 private final JdbcTemplate jdbc; private final String bootstrapUsername;
 AdminUsersController(JdbcTemplate jdbc,@org.springframework.beans.factory.annotation.Value("${APP_ADMIN_USERNAME:}") String bootstrapUsername){this.jdbc=jdbc;this.bootstrapUsername=bootstrapUsername;}
 @GetMapping List<UserView> users(){return jdbc.query("SELECT id,username,role FROM app_users ORDER BY username LIMIT 200",(r,n)->new UserView(r.getString(1),r.getString(2),r.getString(3),!bootstrapUsername.isBlank() && bootstrapUsername.equals(r.getString(2))));}
 @PutMapping("/{id}/role") @ResponseStatus(HttpStatus.NO_CONTENT) void role(Principal admin,@PathVariable String id,@Valid @RequestBody RoleRequest request){
  if(admin.getName().equals(id) && request.role().equals("USER"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"不能撤销当前管理员自己的权限");
  if(request.role().equals("USER") && !bootstrapUsername.isBlank() && jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE id=? AND username=?",Integer.class,id,bootstrapUsername)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"不能撤销初始管理员权限");
  if(jdbc.update("UPDATE app_users SET role=? WHERE id=?",request.role(),id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"用户不存在");
 }
}
