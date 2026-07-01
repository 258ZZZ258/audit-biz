-- ═══ audit-biz 本地 dev seed(让 A4 回查/鉴权本地能真跑) ═══

-- 语料 seed(A4 CitationAssembler 回查这些 chunk_id 会返回完整四级引用)
INSERT INTO doc_versions (doc_version_id, title, doc_number, version_status) VALUES
    ('DV01', '资管产品适当性管理办法', '沪金监规[2024]1号', 'effective'),
    ('DV02', '证券公司客户适当性管理办法', '证监会令[2023]5号', 'effective');

INSERT INTO chunks (chunk_id, doc_version_id, clause_path, page_start, page_end) VALUES
    ('CH0000000000000000000001', 'DV01', '第三章/第十条', 12, 13),
    ('CH0000000000000000000002', 'DV01', '第三章/第十一条', 13, 14),
    ('CH0000000000000000000003', 'DV02', '第二章/第八条', 6, 7);

INSERT INTO cases
    (case_id, case_name, regulator, penalty_date, violation_topic, related_regulation, core_issue, insight)
VALUES
    ('CASE01', '某城商银行理财子公司未有效评估客户风险等级案', '上海证监局', '2024-10-17',
     '适当性评估不足', '资管产品适当性管理办法', '评估要素不完整、主观判断较多',
     '评估要素需完整、客观;评估方法需留痕可追溯');

-- casbin_rule seed(对齐 A2 文件策略 policy.csv;切 JDBC adapter 时生效)
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
    ('p', 'ROLE_AUDIT_MANAGER', 'regulation_query', 'read'),
    ('p', 'ROLE_AUDIT_MANAGER', 'export', 'execute'),
    ('p', 'ROLE_AUDIT_MANAGER', 'template', 'read'),
    ('p', 'ROLE_AUDITOR', 'regulation_query', 'read');

INSERT INTO casbin_rule (ptype, v0, v1) VALUES
    ('g', 'zhang', 'ROLE_AUDIT_MANAGER'),
    ('g', 'li', 'ROLE_AUDITOR');
