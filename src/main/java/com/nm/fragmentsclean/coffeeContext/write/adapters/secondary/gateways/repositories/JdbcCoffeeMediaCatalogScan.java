package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeeMediaCatalogScan;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosArrangedEvent.ArrangedPhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/** Producer-owned technical scan: one consistent statement for parents and their photos. */
@Repository
public class JdbcCoffeeMediaCatalogScan implements CoffeeMediaCatalogScan {
    private final JdbcTemplate jdbc;
    public JdbcCoffeeMediaCatalogScan(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Batch lockNext(int limit){
        var checkpoint=jdbc.query("SELECT cursor_id FROM coffee_media_catalog_scan WHERE id=1 AND next_scan_at<=now() FOR UPDATE SKIP LOCKED",(rs,n)->Optional.ofNullable(rs.getObject(1,UUID.class)));
        if(checkpoint.isEmpty())return new Batch(false,List.of());
        var cursor=checkpoint.getFirst();var args=new ArrayList<Object>();cursor.ifPresent(args::add);args.add(limit);
        String sql="WITH batch AS (SELECT id,version,updated_at FROM coffees"+(cursor.isPresent()?" WHERE id>?":"")+" ORDER BY id LIMIT ?) SELECT batch.*,p.photo_id,p.photo_uri,p.is_cover,p.sort_order FROM batch LEFT JOIN coffee_photos p ON p.coffee_id=batch.id ORDER BY batch.id,p.sort_order";
        var snapshots=jdbc.query(sql,rs->{
            Map<UUID,MutableSnapshot> rows=new LinkedHashMap<>();
            while(rs.next()){
                UUID id=rs.getObject("id",UUID.class);var row=rows.get(id);
                if(row==null){row=new MutableSnapshot(id,rs.getInt("version"),rs.getTimestamp("updated_at").toInstant());rows.put(id,row);}
                UUID photo=rs.getObject("photo_id",UUID.class);
                if(photo!=null)row.photos.add(new ArrangedPhoto(photo,rs.getString("photo_uri"),rs.getBoolean("is_cover"),rs.getInt("sort_order")));
            }
            return rows.values().stream().map(r->new Snapshot(new CoffeeId(r.id),r.photos,r.version,r.at)).toList();
        },args.toArray());
        return new Batch(true,snapshots);
    }
    public void advance(UUID cursor,boolean complete){
        if(complete)jdbc.update("UPDATE coffee_media_catalog_scan SET cursor_id=NULL,next_scan_at=now()+interval '1 day',completed_at=now() WHERE id=1");
        else jdbc.update("UPDATE coffee_media_catalog_scan SET cursor_id=? WHERE id=1",cursor);
    }
    private static final class MutableSnapshot {
        final UUID id;final int version;final Instant at;final List<ArrangedPhoto> photos=new ArrayList<>();
        MutableSnapshot(UUID id,int version,Instant at){this.id=id;this.version=version;this.at=at;}
    }
}
