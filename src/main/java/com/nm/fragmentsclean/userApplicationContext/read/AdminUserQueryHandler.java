package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.Optional;import java.util.UUID;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class AdminUserQueryHandler implements QueryHandler<SearchAdminUsersQuery,AdminUserPage> {
 private final AdminUserReadRepository users;
 public AdminUserQueryHandler(AdminUserReadRepository users){this.users=users;}
 public AdminUserPage handle(SearchAdminUsersQuery query){return users.search(query);}
 public Optional<AdminUserView> byId(UUID id){return users.byId(id);}
}
