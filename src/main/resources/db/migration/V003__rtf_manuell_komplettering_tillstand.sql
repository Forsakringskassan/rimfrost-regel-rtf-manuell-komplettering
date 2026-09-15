CREATE TABLE rtf_manuell_komplettering_tillstand (
    handlaggning_id       UUID         NOT NULL PRIMARY KEY,
    oul_uppgift_id        UUID         NOT NULL,
    reply_to              VARCHAR(255) NOT NULL,
    regel_request_id      UUID         NOT NULL,
    aktivitet_id          UUID         NOT NULL,
    type                  VARCHAR(255) NOT NULL,
    kogitorootprocid      VARCHAR(255) NOT NULL,
    kogitorootprociid     UUID         NOT NULL,
    kogitoparentprociid   UUID         NOT NULL,
    kogitoprocid          VARCHAR(255) NOT NULL,
    kogitoprocinstanceid  UUID         NOT NULL,
    kogitoprocist         VARCHAR(255) NOT NULL,
    kogitoprocversion     VARCHAR(255) NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL
);
