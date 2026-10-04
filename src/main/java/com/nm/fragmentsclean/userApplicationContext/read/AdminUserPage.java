package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.List;
public record AdminUserPage(List<AdminUserView> items,String nextCursor) {}
