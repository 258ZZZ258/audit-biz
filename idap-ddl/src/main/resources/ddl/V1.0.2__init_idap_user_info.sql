-- IDAP 基座用户表。与制度语料表隔离，不参与制度查询写链路。
CREATE TABLE idap_user_info (
    user_id       varchar(64) PRIMARY KEY,
    user_name     varchar(128),
    email         varchar(256),
    mobile        varchar(32),
    sex           smallint DEFAULT 0,
    note          varchar(1024),
    department_id varchar(64),
    role_id       varchar(64),
    status        smallint DEFAULT 1,
    avatar        varchar(512),
    create_time   timestamp,
    create_user   varchar(64),
    update_time   timestamp,
    update_user   varchar(64),
    deleted       smallint DEFAULT 0 NOT NULL
);

CREATE INDEX idx_idap_user_info_name ON idap_user_info (user_name);
CREATE INDEX idx_idap_user_info_mobile ON idap_user_info (mobile);
