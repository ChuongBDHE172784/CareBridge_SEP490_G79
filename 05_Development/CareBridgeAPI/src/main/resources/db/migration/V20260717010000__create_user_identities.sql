CREATE TABLE public.user_identities (
    identity_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id) ON DELETE CASCADE,
    provider varchar(20) NOT NULL CHECK (provider IN ('GOOGLE', 'PHONE')),
    provider_subject varchar(255) NOT NULL,
    provider_email varchar(255),
    provider_phone varchar(30),
    created_at timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_user_identities_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_identities_user_id ON public.user_identities(user_id);
