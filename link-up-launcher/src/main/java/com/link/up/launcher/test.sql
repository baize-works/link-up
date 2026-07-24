CREATE
DATABASE IF NOT EXISTS flux_test
    DEFAULT CHARACTER SET utf8mb4;

USE
flux_test;

CREATE TABLE user_info
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    username   VARCHAR(64) NOT NULL,
    age        INT,
    email      VARCHAR(128),
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO user_info (username, age, email)
VALUES ('张三', 25, 'zhangsan@example.com'),
       ('李四', 30, 'lisi@example.com'),
       ('王五', 28, 'wangwu@example.com'),
       ('赵六', 35, 'zhaoliu@example.com');