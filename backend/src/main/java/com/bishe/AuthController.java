package com.bishe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
class AuthController {
 record Credentials(@NotBlank @Pattern(regexp="^[A-Za-z0-9_]{3,32}$") String username,@NotBlank @Size(min=8,max=72) String password){}
 private final AccountService accounts;
 AuthController(AccountService accounts){this.accounts=accounts;}
 @PostMapping("/register") AccountService.AuthResult register(@Valid @RequestBody Credentials input){return accounts.register(input.username(),input.password());}
 @PostMapping("/login") AccountService.AuthResult login(@Valid @RequestBody Credentials input){return accounts.login(input.username(),input.password());}
}
