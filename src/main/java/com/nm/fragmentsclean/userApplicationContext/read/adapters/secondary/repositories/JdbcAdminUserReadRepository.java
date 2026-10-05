package com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories;
import com.nm.fragmentsclean.userApplicationContext.read.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;
import java.util.*;import java.sql.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;
@Repository public class JdbcAdminUserReadRepository implements AdminUserReadRepository {
 private final JdbcTemplate jdbc;private final PrivateMediaUrlResolver urls;
 public JdbcAdminUserReadRepository(JdbcTemplate jdbc,PrivateMediaUrlResolver urls){this.jdbc=jdbc;this.urls=urls;}
 public AdminUserPage search(SearchAdminUsersQuery q){
  var args=new ArrayList<Object>();args.add(q.q());args.add(q.q());args.add(q.q());
  String sql="SELECT id,display_name,avatar_url,lifecycle_status,created_at,updated_at FROM app_users WHERE (?='' OR position(lower(?) in lower(display_name))>0 OR cast(id as text)=?)";
  if(q.cursor()!=null){sql+=" AND id>?";args.add(q.cursor());}sql+=" ORDER BY id LIMIT ?";args.add(q.limit()+1);
  var rows=jdbc.query(sql,this::row,args.toArray());boolean more=rows.size()>q.limit();var items=List.copyOf(rows.subList(0,Math.min(rows.size(),q.limit())));
  return new AdminUserPage(items,more?items.getLast().userId().toString():null);
 }
 public Optional<AdminUserView> byId(UUID id){return jdbc.query("SELECT id,display_name,avatar_url,lifecycle_status,created_at,updated_at FROM app_users WHERE id=?",this::row,id).stream().findFirst();}
 private AdminUserView row(ResultSet rs,int index)throws SQLException{return new AdminUserView(rs.getObject("id",UUID.class),rs.getString("display_name"),urls.resolve(rs.getString("avatar_url")),rs.getString("lifecycle_status"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant());}
}
