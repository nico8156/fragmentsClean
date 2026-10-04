package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.read.*;
import java.util.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoRetirement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository public class JdbcCoffeeMediaLifecycleReadRepository implements CoffeeMediaLifecycleReadRepository {
 private final JdbcTemplate jdbc;private final com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage storage;private final com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider clock;
 public JdbcCoffeeMediaLifecycleReadRepository(JdbcTemplate jdbc,com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage storage,com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider clock){this.jdbc=jdbc;this.storage=storage;this.clock=clock;}
 public Optional<CoffeeMediaLifecycleView> byId(UUID id){
  var rows=jdbc.query("""
   SELECT p.coffee_id,c.publication_status AS coffee_status,'AVAILABLE' AS status,NULL::timestamptz AS retired_at,p.photo_uri,NULL::timestamptz AS purge_requested_at,NULL::timestamptz AS purged_at
   FROM coffee_photos p JOIN coffees c ON c.id=p.coffee_id WHERE p.photo_id=?
   UNION ALL
   SELECT p.coffee_id,c.publication_status AS coffee_status,p.lifecycle_status AS status,p.retired_at,p.photo_uri,p.purge_requested_at,p.purged_at
   FROM coffee_photo_retirements p JOIN coffees c ON c.id=p.coffee_id WHERE p.photo_id=?
   """,(rs,n)->{
    var at=rs.getTimestamp("retired_at");var retiredAt=at==null?null:at.toInstant();String status=rs.getString("status"),coffeeStatus=rs.getString("coffee_status");boolean mutable=!"ARCHIVED".equals(coffeeStatus);
    var coffee=rs.getObject("coffee_id",UUID.class);var requested=rs.getTimestamp("purge_requested_at");var purged=rs.getTimestamp("purged_at");String ref=rs.getString("photo_uri");boolean supported=storage.canDeletePhoto(new com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId(coffee),id,ref);
    boolean canPurge=mutable&&"RETIRED".equals(status)&&retiredAt!=null&&!clock.now().isBefore(retiredAt.plus(CoffeePhotoRetirement.MINIMUM_RETENTION))&&supported;
    if(canPurge){var refs=storage.referencesForPhoto(new com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId(coffee),id,ref);String slots=String.join(",",java.util.Collections.nCopies(refs.size(),"?"));var args=new java.util.ArrayList<Object>();args.add(id);args.addAll(refs);args.add(id);args.addAll(refs);long uses=jdbc.queryForObject("SELECT (SELECT count(*) FROM coffee_photos WHERE photo_id=? OR trim(photo_uri) IN ("+slots+"))+(SELECT count(*) FROM coffee_photo_retirements WHERE photo_id<>? AND lifecycle_status<>'DELETED' AND trim(photo_uri) IN ("+slots+"))",Long.class,args.toArray());canPurge=uses==0;}
    return new CoffeeMediaLifecycleView(id,coffee,status,coffeeStatus,retiredAt,retiredAt==null?null:retiredAt.plus(CoffeePhotoRetirement.MINIMUM_RETENTION),mutable&&"AVAILABLE".equals(status),mutable&&"RETIRED".equals(status),canPurge,mutable&&"AVAILABLE".equals(status),requested==null?null:requested.toInstant(),purged==null?null:purged.toInstant());
   },id,id);
  // Legacy duplicate identities must not select an arbitrary owner.
  return rows.size()==1?Optional.of(rows.getFirst()):Optional.empty();
 }
}
