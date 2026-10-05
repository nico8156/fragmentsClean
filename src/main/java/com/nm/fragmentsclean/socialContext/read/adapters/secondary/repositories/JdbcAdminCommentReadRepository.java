package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;
import com.nm.fragmentsclean.socialContext.read.*;
import java.sql.Timestamp;import java.util.ArrayList;import java.util.List;import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;
@Repository public class JdbcAdminCommentReadRepository implements AdminCommentReadRepository {
 private final JdbcTemplate jdbc;
 public JdbcAdminCommentReadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public AdminCommentViews.Page search(SearchAdminCommentsQuery query){
  var sql=new StringBuilder("SELECT id,target_id,parent_id,author_id,body,created_at,edited_at,deleted_at,moderation FROM social_comments_projection WHERE author_id=?");
  var args=new ArrayList<Object>();args.add(query.authorId());
  if(query.moderation()!=null){sql.append(" AND moderation=?");args.add(query.moderation());}
  if(query.cursor()!=null){sql.append(" AND (created_at,id) < (?,?)");args.add(Timestamp.from(query.cursor().createdAt()));args.add(query.cursor().id());}
  sql.append(" ORDER BY created_at DESC,id DESC LIMIT ?");args.add(query.limit()+1);
  var rows=jdbc.query(sql.toString(),(rs,n)->new AdminCommentViews.Comment(rs.getObject("id",UUID.class),rs.getObject("target_id",UUID.class),rs.getObject("parent_id",UUID.class),rs.getObject("author_id",UUID.class),rs.getString("body"),rs.getTimestamp("created_at").toInstant(),instant(rs.getTimestamp("edited_at")),instant(rs.getTimestamp("deleted_at")),rs.getString("moderation")),args.toArray());
  boolean more=rows.size()>query.limit();var items=List.copyOf(rows.subList(0,Math.min(rows.size(),query.limit())));
  return new AdminCommentViews.Page(items,more?new AdminCommentCursor(items.getLast().createdAt(),items.getLast().id()).encode():null);
 }
 private static java.time.Instant instant(Timestamp timestamp){return timestamp==null?null:timestamp.toInstant();}
}
