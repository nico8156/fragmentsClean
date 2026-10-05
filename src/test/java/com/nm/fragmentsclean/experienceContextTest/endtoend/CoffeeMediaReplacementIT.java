package com.nm.fragmentsclean.experienceContextTest.endtoend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class CoffeeMediaReplacementIT extends AbstractExperienceE2E {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
 final String admin="99999999-9999-9999-9999-999999999999";
 @Test void replacement_retains_old_photo_and_updates_gallery_in_one_source_command()throws Exception{
  UUID coffee=UUID.randomUUID(),old=UUID.randomUUID(),command=UUID.randomUUID();
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at,publication_status) VALUES(?,'Replacement',0,0,1,now(),'PUBLISHED')",coffee);
  jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,true,0)",coffee,old,"https://images.test/original.jpg");
  var photo=new MockMultipartFile("photo","new.png","image/png",new byte[]{1,2,3});
  mvc.perform(multipart("/api/admin/studio/coffee-media/"+old+"/replacement").file(photo).param("commandId",command.toString()).param("coffeeId",coffee.toString()).param("reason","Nouvelle photo").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isAccepted());
  mvc.perform(multipart("/api/admin/studio/coffee-media/"+old+"/replacement").file(photo).param("commandId",command.toString()).param("coffeeId",coffee.toString()).param("reason","Nouvelle photo").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isAccepted());
  assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM coffee_photo_retirements WHERE photo_id=?",String.class,old)).isEqualTo("RETIRED");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id<>? AND is_cover=true",Integer.class,coffee,old)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT action FROM admin_audit_log WHERE command_id=? AND target_id=?",String.class,command,old)).isEqualTo("COFFEE_MEDIA_REPLACED");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,command)).isEqualTo(2);
  UUID replacement=jdbc.queryForObject("SELECT photo_id FROM coffee_photos WHERE coffee_id=?",UUID.class,coffee);
  var factory=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory();for(var event:outbox.findAll().stream().filter(e->e.getPayloadJson().contains(command.toString())).toList()){router.route(factory.from(event,"coffees-events"));router.route(factory.from(event,"media-catalog-events"));}
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos_projection WHERE coffee_id=? AND id=?",Integer.class,coffee,replacement)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos_projection WHERE coffee_id=? AND id=?",Integer.class,coffee,old)).isZero();
  assertThat(jdbc.queryForObject("SELECT status FROM media_catalog_entries WHERE origin='COFFEE' AND media_id=?",String.class,old)).isEqualTo("DELETION_PENDING");
  assertThat(jdbc.queryForObject("SELECT status FROM media_catalog_entries WHERE origin='COFFEE' AND media_id=?",String.class,replacement)).isEqualTo("AVAILABLE");

 }
 @Test void replacement_requires_admin_and_reason_and_rejects_foreign_owner()throws Exception{
  UUID coffee=UUID.randomUUID(),old=UUID.randomUUID();jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES(?,'Guard',0,0,1,now())",coffee);jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,true,0)",coffee,old,"https://images.test/original.jpg");String path="/api/admin/studio/coffee-media/"+old+"/replacement";var photo=new MockMultipartFile("photo","new.png","image/png",new byte[]{1});
  mvc.perform(multipart(path).file(photo).param("commandId",UUID.randomUUID().toString()).param("coffeeId",coffee.toString()).param("reason","Review")).andExpect(status().isUnauthorized());
  mvc.perform(multipart(path).file(photo).param("commandId",UUID.randomUUID().toString()).param("coffeeId",coffee.toString()).param("reason","Review").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
  mvc.perform(multipart(path).file(photo).param("commandId",UUID.randomUUID().toString()).param("coffeeId",coffee.toString()).param("reason"," ").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isUnprocessableEntity());
  UUID foreignCoffee=UUID.randomUUID();jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES(?,'Foreign owner',0,0,1,now())",foreignCoffee);
  mvc.perform(multipart(path).file(photo).param("commandId",UUID.randomUUID().toString()).param("coffeeId",foreignCoffee.toString()).param("reason","Review").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isUnprocessableEntity());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,coffee,old)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photo_retirements WHERE photo_id=?",Integer.class,old)).isZero();
 }

 @Test void failed_source_write_rolls_back_gallery_retirement_events_and_audit()throws Exception{
  UUID coffee=UUID.randomUUID(),old=UUID.randomUUID(),command=UUID.randomUUID();jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES(?,'Rollback',0,0,1,now())",coffee);jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,true,0)",coffee,old,"https://images.test/original.jpg");
  var replacement=com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage.photoId(new com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId(coffee),"admin-upload/"+command+"/new.png");String constraint="test_coffee_replace_"+coffee.toString().replace("-","");jdbc.execute("ALTER TABLE coffee_photos ADD CONSTRAINT "+constraint+" CHECK (photo_id<>'"+replacement+"'::uuid) NOT VALID");
  try{var photo=new MockMultipartFile("photo","new.png","image/png",new byte[]{1});org.assertj.core.api.Assertions.assertThatThrownBy(()->mvc.perform(multipart("/api/admin/studio/coffee-media/"+old+"/replacement").file(photo).param("commandId",command.toString()).param("coffeeId",coffee.toString()).param("reason","New cover").with(jwt().jwt(j->j.subject(admin))))).hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,coffee,old)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photo_retirements WHERE photo_id=?",Integer.class,old)).isZero();assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,command)).isZero();assertThat(outbox.findAll().stream().filter(e->e.getPayloadJson().contains(command.toString()))).isEmpty();
  }finally{jdbc.execute("ALTER TABLE coffee_photos DROP CONSTRAINT "+constraint);}
 }

}
