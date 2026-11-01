-- Catalog rows predate pricing approval. None may become public through deployment alone.
ALTER TABLE saas_plan_versions
    ADD COLUMN public_visible boolean NOT NULL DEFAULT false;
