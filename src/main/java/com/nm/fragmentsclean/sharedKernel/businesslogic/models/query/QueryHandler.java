package com.nm.fragmentsclean.sharedKernel.businesslogic.models.query;

public interface QueryHandler <Q extends Query<R>, R> {
    R handle(Q query);
}
