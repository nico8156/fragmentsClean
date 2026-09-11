package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import java.time.Instant;import java.util.Map;import java.util.UUID;
public record AvatarUploadIntent(UUID mediaId,boolean uploadRequired,String uploadUrl,String method,Map<String,String> headers,Instant expiresAt){}
