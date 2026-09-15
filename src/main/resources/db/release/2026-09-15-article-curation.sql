ALTER TABLE articles ADD COLUMN IF NOT EXISTS featured_rank INTEGER;
ALTER TABLE articles_projection ADD COLUMN IF NOT EXISTS featured_rank INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uq_articles_featured_rank
    ON articles(featured_rank) WHERE status = 'PUBLISHED' AND featured_rank IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_articles_projection_featured
    ON articles_projection(locale, featured_rank)
    WHERE status = 'published' AND featured_rank IS NOT NULL;
