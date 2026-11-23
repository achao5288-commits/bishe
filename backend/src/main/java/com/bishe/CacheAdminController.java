package com.bishe;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/cache")
class CacheAdminController {
 private final RedisCache cache;
 CacheAdminController(RedisCache cache){this.cache=cache;}
 @PostMapping("/invalidate") @ResponseStatus(HttpStatus.NO_CONTENT) void invalidate(){cache.invalidate();}
}
