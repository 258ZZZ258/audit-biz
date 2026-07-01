-- ═══ audit-biz 本地 dev PG schema ═══
-- 注:chunks / doc_versions / cases 是 audit-ai Alembic 管的**权威语料表**;此处仅为本地 dev 回查造
--     **最小 stand-in**(列取 biz 回查所需子集,可能少于真表)。真集成(I1)连共享 PG,不用本 DDL。
--     casbin_rule 是 **biz 自有**(jCasbin JDBC adapter 用;A2 现用文件策略,切 PG 时启用)。

-- 语料回查表(A4:CitationMapper 按 chunk_id 读 chunks⋈doc_versions)
CREATE TABLE doc_versions (
    doc_version_id varchar(26) PRIMARY KEY,
    title          varchar(512),
    doc_number     varchar(128),
    version_status varchar(16)
);

CREATE TABLE chunks (
    chunk_id       varchar(24) PRIMARY KEY,
    doc_version_id varchar(26) REFERENCES doc_versions (doc_version_id),
    clause_path    varchar(512),
    page_start     int,
    page_end       int
);

-- 案例表(未来 case 回查用;列近似产品原型,真表列以 audit-ai 为准)
CREATE TABLE cases (
    case_id            varchar(26) PRIMARY KEY,
    case_name          varchar(512),
    regulator          varchar(128),
    penalty_date       date,
    violation_topic    varchar(256),
    related_regulation varchar(512),
    core_issue         text,
    insight            text
);

-- jCasbin JDBC adapter 标准表(biz 自有;切 PG 策略时用)
CREATE TABLE casbin_rule (
    id    bigserial PRIMARY KEY,
    ptype varchar(100),
    v0    varchar(100),
    v1    varchar(100),
    v2    varchar(100),
    v3    varchar(100),
    v4    varchar(100),
    v5    varchar(100)
);
