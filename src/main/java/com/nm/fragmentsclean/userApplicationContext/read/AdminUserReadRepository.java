package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.Optional;import java.util.UUID;
public interface AdminUserReadRepository {
 AdminUserPage search(SearchAdminUsersQuery query);
 Optional<AdminUserView> byId(UUID id);
}
