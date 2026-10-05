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
    public Optional<UUID> ownerOf(UUID id){return entities.createQuery("SELECT r.coffeeId FROM CoffeePhotoRetirementJpaEntity r WHERE r.photoId=:id",UUID.class).setParameter("id",id).getResultStream().findFirst();}
    public List<UUID> pendingPurgeIds(int limit){if(limit<1||limit>100)throw new IllegalArgumentException("Purge batch must be 1 to 100");return entities.createQuery("SELECT r.photoId FROM CoffeePhotoRetirementJpaEntity r WHERE r.lifecycleStatus='DELETION_PENDING' ORDER BY r.purgeRequestedAt,r.photoId",UUID.class).setMaxResults(limit).getResultList();}
    public long referencesTo(UUID id,List<String> refs){
        if(refs.isEmpty())return currentPhotoCount(id);
        long active=entities.createQuery("SELECT count(p) FROM CoffeeJpaEntity c JOIN c.photos p WHERE p.photoId=:id OR trim(p.photoUri) IN :refs",Long.class).setParameter("id",id).setParameter("refs",refs).getSingleResult();
        long retained=entities.createQuery("SELECT count(r) FROM CoffeePhotoRetirementJpaEntity r WHERE r.photoId<>:id AND r.lifecycleStatus<>'DELETED' AND trim(r.photoUri) IN :refs",Long.class).setParameter("id",id).setParameter("refs",refs).getSingleResult();return active+retained;
    }
    public Optional<CoffeePhotoRetirement> byId(UUID id){return repository.findById(id).map(e->new CoffeePhotoRetirement(new Photo(new PhotoId(e.photoId),new CoffeeId(e.coffeeId),e.photoUri,e.wasCover,e.sortOrder),e.retiredAt,e.lifecycleStatus,e.purgeRequestedAt,e.purgedAt,e.purgeCommandId));}
    public List<CoffeePhotoRetirement> byCoffee(UUID id){return repository.findByCoffeeId(id).stream().map(e->new CoffeePhotoRetirement(new Photo(new PhotoId(e.photoId),new CoffeeId(e.coffeeId),e.photoUri,e.wasCover,e.sortOrder),e.retiredAt,e.lifecycleStatus,e.purgeRequestedAt,e.purgedAt,e.purgeCommandId)).toList();}
    public void save(CoffeePhotoRetirement item){var photo=item.photo();var e=new CoffeePhotoRetirementJpaEntity();e.photoId=photo.id().value();e.coffeeId=photo.coffeeId().value();e.photoUri=photo.uri();e.wasCover=photo.isCover();e.sortOrder=photo.sortOrder();e.retiredAt=item.retiredAt();e.lifecycleStatus=item.status();e.purgeRequestedAt=item.purgeRequestedAt();e.purgedAt=item.purgedAt();e.purgeCommandId=item.purgeCommandId();repository.save(e);}
    public void remove(UUID id){repository.deleteById(id);}
    public boolean hasForCoffee(UUID id){return repository.existsByCoffeeId(id);}
    public long currentPhotoCount(UUID id){return entities.createQuery("SELECT count(p) FROM CoffeeJpaEntity c JOIN c.photos p WHERE p.photoId=:id",Long.class).setParameter("id",id).getSingleResult();}
}
