package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record SearchAdminUsersQuery(String q,UUID cursor,int limit) implements Query<AdminUserPage> {
 public SearchAdminUsersQuery {q=q==null?"":q.strip();if(q.length()>200||limit<1||limit>100)throw new IllegalArgumentException("Invalid search or page size");}
}
