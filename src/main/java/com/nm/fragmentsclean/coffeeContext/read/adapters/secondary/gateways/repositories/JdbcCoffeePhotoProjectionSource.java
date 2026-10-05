package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository public class JdbcCoffeePhotoProjectionSource implements CoffeePhotoProjectionSource {
 private final JdbcTemplate jdbc;
 public JdbcCoffeePhotoProjectionSource(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<Snapshot> current(UUID id){
  // The caller's transaction holds this lock through projection persistence.
  // Source writers all take FOR UPDATE before loading their Coffee aggregate.
  var parent=jdbc.query("SELECT publication_status,version,updated_at FROM coffees WHERE id=? FOR SHARE",(rs,n)->new Snapshot(id,rs.getString("publication_status"),rs.getLong("version"),rs.getTimestamp("updated_at").toInstant(),List.of()),id);
  if(parent.isEmpty())return Optional.empty();
  var row=parent.getFirst();
  var photos=jdbc.query("SELECT photo_id,photo_uri,is_cover,sort_order FROM coffee_photos WHERE coffee_id=? ORDER BY sort_order,photo_id",(rs,n)->new CoffeePhotoView(rs.getObject("photo_id",UUID.class),id,rs.getString("photo_uri"),rs.getBoolean("is_cover"),rs.getInt("sort_order")),id);
  return Optional.of(new Snapshot(id,row.publicationStatus(),row.version(),row.changedAt(),photos));
 }
}
