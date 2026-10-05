package com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories;
import com.nm.fragmentsclean.experienceContext.read.*;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.*;
import java.util.*;import java.sql.*;import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;
@Repository public class JdbcAdminExperienceReadRepository implements AdminExperienceReadRepository {
 private final JdbcTemplate jdbc;private final PrivateMediaUrlResolver urls;
 public JdbcAdminExperienceReadRepository(JdbcTemplate jdbc,PrivateMediaUrlResolver urls){this.jdbc=jdbc;this.urls=urls;}
 private static final String SELECT="SELECT v.*,p.display_name,p.avatar_url FROM experience_views v LEFT JOIN experience_user_profiles p ON p.user_id=v.user_id";
 public AdminExperienceViews.UserActions userActions(ListUserExperienceActionsQuery q){
  var sql=new StringBuilder("SELECT a.* FROM experience_moderation_actions_projection a JOIN experience_views v ON v.experience_id=a.experience_id WHERE v.user_id=?");
  var args=new ArrayList<Object>();args.add(q.authorId());
  cursor(sql,args,q.cursor(),"a.occurred_at","a.action_id");sql.append(" ORDER BY a.occurred_at DESC,a.action_id DESC LIMIT ?");args.add(q.limit()+1);
  var rows=jdbc.query(sql.toString(),(rs,n)->new AdminExperienceViews.UserAction(rs.getObject("action_id",UUID.class),rs.getObject("experience_id",UUID.class),rs.getObject("operator_id",UUID.class),rs.getString("decision"),rs.getString("reason"),rs.getTimestamp("occurred_at").toInstant()),args.toArray());
  boolean more=rows.size()>q.limit();var items=List.copyOf(rows.subList(0,Math.min(rows.size(),q.limit())));
  return new AdminExperienceViews.UserActions(items,more?new ExperienceCursor(items.getLast().occurredAt(),items.getLast().actionId()).encode():null);
 }
 public ExperiencePage search(SearchAdminExperiencesQuery q){
  var sql=new StringBuilder(SELECT+" WHERE (?='' OR position(lower(?) in lower(v.message))>0 OR cast(v.experience_id as text)=?)");
  var args=new ArrayList<Object>(List.of(q.q(),q.q(),q.q()));
  if(q.authorId()!=null){sql.append(" AND v.user_id=?");args.add(q.authorId());}
  if(q.moderation()!=null){sql.append(" AND v.moderation_status=?");args.add(q.moderation());}
  if(q.publication()!=null){sql.append(" AND v.publication_status=?");args.add(q.publication());}
  cursor(sql,args,q.cursor(),"v.created_at","v.experience_id");sql.append(" ORDER BY v.created_at DESC,v.experience_id DESC LIMIT ?");args.add(q.limit()+1);
  var rows=jdbc.query(sql.toString(),this::experience,args.toArray());boolean more=rows.size()>q.limit();var items=List.copyOf(rows.subList(0,Math.min(rows.size(),q.limit())));
  return new ExperiencePage(items,more?new ExperienceCursor(items.getLast().createdAt(),items.getLast().experienceId()).encode():null,Instant.now());
 }
 public Optional<AdminExperienceViews.Detail> byId(UUID id){
  return jdbc.query(SELECT+" WHERE v.experience_id=?",this::experience,id).stream().findFirst().map(v->{
   var media=jdbc.query("SELECT * FROM experience_media_views WHERE experience_id=? ORDER BY position,media_id",this::mediaRow,id);
   var audit=actions(id,null,30);return new AdminExperienceViews.Detail(v,media,audit.items(),audit.nextCursor());
  });
 }
 public Optional<AdminExperienceViews.Media> media(UUID id){return jdbc.query("SELECT * FROM experience_media_views WHERE media_id=?",this::mediaRow,id).stream().findFirst();}
 public AdminExperienceViews.Actions actions(UUID id,ExperienceCursor cursor,int limit){
  var sql=new StringBuilder("SELECT * FROM experience_moderation_actions_projection WHERE experience_id=?");var args=new ArrayList<Object>();args.add(id);
  cursor(sql,args,cursor,"occurred_at","action_id");sql.append(" ORDER BY occurred_at DESC,action_id DESC LIMIT ?");args.add(limit+1);
  var rows=jdbc.query(sql.toString(),(rs,n)->new ExperienceModerationActionView(rs.getObject("action_id",UUID.class),rs.getObject("operator_id",UUID.class),rs.getString("decision"),rs.getString("reason"),rs.getTimestamp("occurred_at").toInstant()),args.toArray());
  boolean more=rows.size()>limit;var items=List.copyOf(rows.subList(0,Math.min(rows.size(),limit)));
  return new AdminExperienceViews.Actions(items,more?new ExperienceCursor(items.getLast().occurredAt(),items.getLast().actionId()).encode():null);
 }
 private ExperienceView experience(ResultSet rs,int n)throws SQLException{return new ExperienceView(rs.getObject("experience_id",UUID.class),rs.getObject("coffee_id",UUID.class),rs.getObject("user_id",UUID.class),Objects.requireNonNullElse(rs.getString("display_name"),"Utilisateur"),urls.resolve(rs.getString("avatar_url")),rs.getString("message"),rs.getString("publication_status"),rs.getString("moderation_status"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("version"),List.of());}
 private AdminExperienceViews.Media mediaRow(ResultSet rs,int n)throws SQLException{
  String key=rs.getString("object_key"),status=rs.getString("status");
  String url="AVAILABLE".equals(status)&&key!=null?urls.resolve(PrivateMediaReferences.experience(key)):null;
  return new AdminExperienceViews.Media(rs.getObject("media_id",UUID.class),rs.getObject("experience_id",UUID.class),rs.getObject("user_id",UUID.class),status,url,rs.getString("content_type"),rs.getLong("size_bytes"),rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),rs.getTimestamp("updated_at").toInstant());
 }
 private static void cursor(StringBuilder sql,List<Object> args,ExperienceCursor c,String time,String id){if(c!=null){sql.append(" AND ("+time+","+id+") < (?,?)");args.add(Timestamp.from(c.createdAt()));args.add(c.id());}}
}
