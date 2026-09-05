package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;

public interface ArticleAuthoringObservability {
    void generationRequested(ArticleAuthoringTrigger trigger);
    void leaseClaimed(boolean recovered);
    void generationCompleted();
    void generationFailed(String category);

    static ArticleAuthoringObservability noop() {
        return new ArticleAuthoringObservability() {
            public void generationRequested(ArticleAuthoringTrigger trigger) { }
            public void leaseClaimed(boolean recovered) { }
            public void generationCompleted() { }
            public void generationFailed(String category) { }
        };
    }
}
