package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.*;
public interface AdminAvatarMediaPreviewRepository { Map<UUID,String> currentPreviews(Map<UUID,UUID> mediaProfiles); }
