CREATE TABLE content_sources (
    content_item_id UUID NOT NULL REFERENCES content_items(content_item_id) ON DELETE CASCADE,
    source_title VARCHAR(500) NOT NULL,
    source_url VARCHAR(2000),
    source_publisher VARCHAR(255)
);
CREATE INDEX idx_content_sources_content_item ON content_sources(content_item_id);
