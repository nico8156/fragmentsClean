package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.userApplicationContext.read.*;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/users") public final class ReadAdminUserController {
 private final AdminUserQueryHandler users;
 public ReadAdminUserController(AdminUserQueryHandler users){this.users=users;}
 @GetMapping public AdminUserPage list(@RequestParam(defaultValue="")String q,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="30")int limit){return users.handle(new SearchAdminUsersQuery(q,cursor,limit));}
 @GetMapping("/{id}") public ResponseEntity<AdminUserView> detail(@PathVariable UUID id){return ResponseEntity.of(users.byId(id));}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(){return Map.of("error","INVALID_QUERY");}
}
