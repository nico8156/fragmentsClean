package com.nm.fragmentsclean.experienceContext.read.projections;
import java.util.UUID;
public record ExperienceMediaView(UUID mediaId,String url,Integer width,Integer height,int position,String status){
  public ExperienceMediaView(UUID mediaId,String url,Integer width,Integer height,int position){this(mediaId,url,width,height,position,"AVAILABLE");}
}
