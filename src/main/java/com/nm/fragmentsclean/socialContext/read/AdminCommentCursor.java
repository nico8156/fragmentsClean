package com.nm.fragmentsclean.socialContext.read;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Admin keyset cursor preserves PostgreSQL timestamp precision. Public cursors are unchanged. */
public record AdminCommentCursor(Instant createdAt, UUID id) {
 public static AdminCommentCursor parse(String value) {
  if(value==null||value.isBlank())return null;
  try {
   if(value.length()>512)throw new IllegalArgumentException();
   String decoded=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8);
   int split=decoded.lastIndexOf('|');
   return new AdminCommentCursor(Instant.parse(decoded.substring(0,split)),UUID.fromString(decoded.substring(split+1)));
  }catch(RuntimeException error){throw new IllegalArgumentException("Invalid admin comment cursor");}
 }
 public String encode(){return Base64.getUrlEncoder().withoutPadding().encodeToString((createdAt+"|"+id).getBytes(StandardCharsets.UTF_8));}
}
