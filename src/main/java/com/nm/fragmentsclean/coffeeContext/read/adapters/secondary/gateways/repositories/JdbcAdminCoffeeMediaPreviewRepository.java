package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.read.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcAdminCoffeeMediaPreviewRepository implements AdminCoffeeMediaPreviewRepository {
    private final JdbcTemplate jdbc;private final CoffeePhotoUriResolver resolver;
    public JdbcAdminCoffeeMediaPreviewRepository(JdbcTemplate jdbc,CoffeePhotoUriResolver resolver){this.jdbc=jdbc;this.resolver=resolver;}
    public Map<UUID,String> currentPreviews(Map<UUID,UUID> mediaResources){
        if(mediaResources.isEmpty())return Map.of();
        var clauses=new ArrayList<String>();var args=new ArrayList<Object>();
        mediaResources.forEach((media,coffee)->{clauses.add("(photo_id=? AND coffee_id=?)");args.add(media);args.add(coffee);});
        Map<UUID,String> result=new HashMap<>();
        jdbc.query("SELECT photo_id,photo_uri FROM coffee_photos WHERE "+String.join(" OR ",clauses),rs->{
            String url=resolver.resolve(rs.getString("photo_uri"));
            if(url!=null && (url.matches("https?://.+") || url.matches("/api/coffees/photo-assets/[a-zA-Z0-9_-]+[.](jpg|jpeg|png|webp|gif)")))result.put(rs.getObject("photo_id",UUID.class),url);
        },args.toArray());
        return Map.copyOf(result);
    }
}
