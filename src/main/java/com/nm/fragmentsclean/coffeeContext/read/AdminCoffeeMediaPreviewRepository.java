package com.nm.fragmentsclean.coffeeContext.read;
import java.util.*;
public interface AdminCoffeeMediaPreviewRepository { Map<UUID,String> currentPreviews(Map<UUID,UUID> mediaResources); }
