package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage;
import java.util.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
/** Pure locator for the references emitted by Coffee storage; no bucket discovery. */
public final class CoffeeManagedPhotoReference {
 private CoffeeManagedPhotoReference(){}
 public static String prefix(String value){String p=value==null||value.isBlank()?"fragments/staging/coffees":value.trim();return p.replaceAll("^/+|/+$","");}
 public static Optional<String> s3Key(CoffeePhotoStorageProperties properties,CoffeeId coffee,UUID photo,String reference){
  if(coffee==null||photo==null||reference==null||properties.getS3Bucket()==null||properties.getS3Bucket().isBlank())return Optional.empty();
  String start="s3://"+properties.getS3Bucket().trim()+"/"+prefix(properties.getS3Prefix())+"/"+coffee.value()+"/photos/";
  if(!reference.startsWith(start)||!reference.substring(start.length()).matches(photo+"\\.(jpg|png|webp|gif)"))return Optional.empty();
  return Optional.of(reference.substring(("s3://"+properties.getS3Bucket().trim()+"/").length()));
 }
 public static Optional<String> localFile(CoffeePhotoStorageProperties properties,CoffeeId coffee,UUID photo,String reference){
  if(coffee==null||photo==null||reference==null)return Optional.empty();String path="/api/coffees/photo-assets/";String base=properties.getPublicBaseUrl()==null?"":properties.getPublicBaseUrl().trim().replaceAll("/+$","");
  String file=reference.startsWith(path)?reference.substring(path.length()):!base.isBlank()&&reference.startsWith(base+path)?reference.substring((base+path).length()):null;
  return file!=null&&file.matches(photo+"\\.(jpg|png|webp|gif)")?Optional.of(file):Optional.empty();
 }
}
