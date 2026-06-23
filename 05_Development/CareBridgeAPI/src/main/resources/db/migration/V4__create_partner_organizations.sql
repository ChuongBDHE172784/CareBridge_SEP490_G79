-- V4: Recreate partner_organizations for UC-118 Create Partner Profile
-- Drops old auto-generated schema and rebuilds with UC-118 columns and constraints.

DROP TABLE IF EXISTS partner_organizations CASCADE;

CREATE TABLE partner_organizations (
    partner_id      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200)  NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    address         VARCHAR(500)  NOT NULL,
    city            VARCHAR(100)  NOT NULL,
    phone           VARCHAR(20)   NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    website         VARCHAR(500),
    logo_url        VARCHAR(1000),
    description     VARCHAR(2000),
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING_APPROVAL',
    representative_user_id BIGINT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_partner_representative UNIQUE (representative_user_id),
    CONSTRAINT uk_partner_email          UNIQUE (email)
);

CREATE INDEX idx_partner_org_status ON partner_organizations (status);
CREATE INDEX idx_partner_org_type   ON partner_organizations (type);
