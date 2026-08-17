-- ============================================================================
-- Migration V3: Create maternal_knowledge_chunks table for AI RAG Service
-- ============================================================================

-- 1. Enable pgvector extension if available
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create maternal_knowledge_chunks table
CREATE TABLE IF NOT EXISTS public.maternal_knowledge_chunks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    stage VARCHAR(50) NOT NULL DEFAULT 'ALL',
    topic VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    source VARCHAR(255) NOT NULL,
    section VARCHAR(255),
    content TEXT NOT NULL,
    chunk_index INTEGER DEFAULT 0,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Standard B-Tree Indexes for fast filtering by stage, topic, title
CREATE INDEX IF NOT EXISTS idx_maternal_chunks_title ON public.maternal_knowledge_chunks(title);
CREATE INDEX IF NOT EXISTS idx_maternal_chunks_stage ON public.maternal_knowledge_chunks(stage);
CREATE INDEX IF NOT EXISTS idx_maternal_chunks_topic ON public.maternal_knowledge_chunks(topic);

-- 4. HNSW Vector Index for ultra-fast Cosine similarity search (<=> operator)
CREATE INDEX IF NOT EXISTS idx_maternal_chunks_embedding_hnsw 
ON public.maternal_knowledge_chunks 
USING hnsw (embedding vector_cosine_ops);
