package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.read.*;
import java.util.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoRetirement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository public class JdbcCoffeeMediaLifecycleReadRepository implements CoffeeMediaLifecycleReadRepository {
 private final JdbcTemplate jdbc;
 public JdbcCoffeeMediaLifecycleReadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<CoffeeMediaLifecycleView> byId(UUID id){
  var rows=jdbc.query("""
   SELECT p.coffee_id,c.publication_status AS coffee_status,'AVAILABLE' AS status,NULL::timestamptz AS retired_at
   FROM coffee_photos p JOIN coffees c ON c.id=p.coffee_id WHERE p.photo_id=?
   UNION ALL
   SELECT p.coffee_id,c.publication_status AS coffee_status,'RETIRED' AS status,p.retired_at
   FROM coffee_photo_retirements p JOIN coffees c ON c.id=p.coffee_id WHERE p.photo_id=?
   """,(rs,n)->{
    var at=rs.getTimestamp("retired_at");var retiredAt=at==null?null:at.toInstant();String status=rs.getString("status"),coffeeStatus=rs.getString("coffee_status");boolean mutable=!"ARCHIVED".equals(coffeeStatus);
    return new CoffeeMediaLifecycleView(id,rs.getObject("coffee_id",UUID.class),status,coffeeStatus,retiredAt,retiredAt==null?null:retiredAt.plus(CoffeePhotoRetirement.MINIMUM_RETENTION),mutable && "AVAILABLE".equals(status),mutable && "RETIRED".equals(status));
   },id,id);
  // Legacy duplicate identities must not select an arbitrary owner.
  return rows.size()==1?Optional.of(rows.getFirst()):Optional.empty();
 }
}
