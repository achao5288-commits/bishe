package com.bishe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
class AdminBootstrap {
 @Bean ApplicationRunner bootstrap(AccountService accounts,@Value("${APP_ADMIN_USERNAME:}") String username,@Value("${APP_ADMIN_PASSWORD:}") String password){return args->{
  if(username.isBlank() && password.isBlank())return;
  if(!username.matches("[A-Za-z0-9_]{3,32}") || password.length()<8 || password.length()>72)throw new IllegalStateException("管理员账号或密码配置无效");
  var existing=accounts.find(username);
  if(existing==null)accounts.create(username,password,"ADMIN");
  else if(!existing.role().equals("ADMIN"))throw new IllegalStateException("指定的管理员用户名已被普通用户占用");
 };}
}
