package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import java.time.Instant;
import java.util.UUID;
public interface ArticleRevisionMaterializer { void materialize(UUID articleId, UUID revisionId, GeneratedArticleDraft draft, Instant now); }
