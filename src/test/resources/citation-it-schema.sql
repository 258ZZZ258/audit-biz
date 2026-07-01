-- A4 集成测试最小 schema:仅 CitationMapper 回查用到的列(真库由 audit-ai Alembic 管,列更全)。
CREATE TABLE doc_versions (
    doc_version_id varchar(26) PRIMARY KEY,
    title varchar(512),
    doc_number varchar(128),
    version_status varchar(16)
);
CREATE TABLE chunks (
    chunk_id varchar(24) PRIMARY KEY,
    doc_version_id varchar(26),
    clause_path varchar(512),
    page_start int,
    page_end int
);

INSERT INTO doc_versions (doc_version_id, title, doc_number, version_status)
VALUES ('DV01', '资管产品适当性管理办法', '沪金监规[2024]1号', 'effective');

INSERT INTO chunks (chunk_id, doc_version_id, clause_path, page_start, page_end)
VALUES ('CH0000000000000000000001', 'DV01', '第三章/第十条', 12, 13);
