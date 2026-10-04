package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoRetirementRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeePhotoRetirementJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository public class JpaCoffeePhotoRetirementRepository implements CoffeePhotoRetirementRepository {
    private final SpringCoffeePhotoRetirementRepository repository;
    private final EntityManager entities;
    public JpaCoffeePhotoRetirementRepository(SpringCoffeePhotoRetirementRepository repository,EntityManager entities){this.repository=repository;this.entities=entities;}
    public Optional<CoffeePhotoRetirement> byId(UUID id){return repository.findById(id).map(e->new CoffeePhotoRetirement(new Photo(new PhotoId(e.photoId),new CoffeeId(e.coffeeId),e.photoUri,e.wasCover,e.sortOrder),e.retiredAt));}
    public void save(CoffeePhotoRetirement item){var photo=item.photo();var e=new CoffeePhotoRetirementJpaEntity();e.photoId=photo.id().value();e.coffeeId=photo.coffeeId().value();e.photoUri=photo.uri();e.wasCover=photo.isCover();e.sortOrder=photo.sortOrder();e.retiredAt=item.retiredAt();repository.save(e);}
    public void remove(UUID id){repository.deleteById(id);}
    public boolean hasForCoffee(UUID id){return repository.existsByCoffeeId(id);}
    public long currentPhotoCount(UUID id){return entities.createQuery("SELECT count(p) FROM CoffeeJpaEntity c JOIN c.photos p WHERE p.photoId=:id",Long.class).setParameter("id",id).getSingleResult();}
}
