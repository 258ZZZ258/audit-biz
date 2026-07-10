-- A4 集成测试最小 schema:仅 CitationMapper 回查用到的列(真库由 audit-ai Alembic 管,列更全)。
CREATE TABLE doc_versions (
    doc_version_id varchar(26) PRIMARY KEY,
    title varchar(512),
    doc_number varchar(128),
    source_filename varchar(512),
    version_status varchar(16)
);
CREATE TABLE chunks (
    chunk_id varchar(24) PRIMARY KEY,
    doc_version_id varchar(26),
    clause_path varchar(512),
    page_start int,
    page_end int,
    text text
);
CREATE TABLE operation_logs (
    id bigserial PRIMARY KEY,
    trace_id varchar(64),
    actor varchar(128),
    action varchar(64),
    action_point varchar(128),
    status varchar(32),
    detail_json text,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO doc_versions (doc_version_id, title, doc_number, source_filename, version_status)
VALUES ('DV01', '资管产品适当性管理办法', '沪金监规[2024]1号', 'D0001_001__沪金监规[2024]1号_资管产品适当性管理办法.pdf', 'effective');

INSERT INTO chunks (chunk_id, doc_version_id, clause_path, page_start, page_end, text)
VALUES ('CH0000000000000000000001', 'DV01', '第三章/第十条', 12, 13, '金融机构应当及时更新客户风险等级。');

INSERT INTO doc_versions (doc_version_id, title, doc_number, source_filename, version_status)
VALUES ('DV02', '', '证监会令第211号', 'D0008_013__证监会令第211号_北京证券交易所上市公司证券发行注册管理办法.pdf', 'effective');

INSERT INTO chunks (chunk_id, doc_version_id, clause_path, page_start, page_end, text)
VALUES ('CH0000000000000000000002', 'DV02', '第五章/第七十条', 20, 20, '中国证监会可以采取监管措施。');
