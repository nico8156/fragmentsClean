package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.openai;

import java.util.List;

/** Technical provider DTO. Public fields are intentional and never cross the ACL mapper. */
public final class OpenAiArticleResponseV1 {
    public String schemaVersion;
    public String title;
    public String introduction;
    public String conclusion;
    public String coverVisualBrief;
    public List<String> tags;
    public List<Section> sections;

    public static final class Section {
        public String heading;
        public String paragraph;
        public String visualBrief;
    }
}
