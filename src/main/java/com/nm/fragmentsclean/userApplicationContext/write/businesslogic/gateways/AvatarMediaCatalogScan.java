package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
import java.util.*;
public interface AvatarMediaCatalogScan {
    record Item(AvatarMedia.Snapshot media,UUID profileUserId){}
    record Batch(boolean due,List<Item> items){}
    Batch lockNext(int limit);
    void advance(UUID lastId,boolean complete);
}
