-- 目标主题订阅关系表 : 用于描述消费者客户端接收主题消息的关系；topic_subscribe_rel_8 => tc_topic_sub_8
CREATE TABLE tc_topic_sub_8 (
  client_id varchar(32) NOT NULL,
  dest_topic_id varchar(32) NOT NULL,
  client_pswd char(32) DEFAULT NULL ,
  max_request int DEFAULT NULL,
  min_timeout int DEFAULT NULL,
  max_timeout int DEFAULT NULL,
  use_status char(1) NOT NULL,
  login_no char(32) DEFAULT NULL ,
  opr_time datetime DEFAULT NULL,
  note varchar(2048) DEFAULT NULL ,
  consume_speed_limit int DEFAULT 0,
  max_messages int DEFAULT 10000,
  warn_messages int DEFAULT 1000,
  part_count int DEFAULT 1,
  part_num_start int DEFAULT 1,
  PRIMARY KEY (client_id,dest_topic_id)
);

INSERT INTO tc_topic_sub_8 (client_id, dest_topic_id, client_pswd,
    max_request, min_timeout, max_timeout, use_status, login_no, opr_time, note,
    consume_speed_limit, max_messages, warn_messages, part_count) VALUES
	('notice_sub_1', 'notice_1', '_null', 20, 60, 600, '1', 'admin', now(), NULL, 0, 10000, 1000, 1, 1);

INSERT INTO tc_topic_sub_8 VALUES
	('Sub108', 'TUrStatusToOboss', '_null', 20, 60, 600, '1', 'admin', now(), NULL, 0, 10000, 1000, 1, 1);
INSERT INTO tc_topic_sub_8 VALUES
	('Sub119Opr', 'TRecOprCnttDest', '1', 10, 60, 600, '1', 'a', now(), 'aa', 0, 300, 100, 7, 1);


-- 主题映射表
CREATE TABLE tc_topic_map_8 (
  src_topic_id varchar(32) NOT NULL ,
  attribute_key varchar(32) NOT NULL,
  attribute_value varchar(32) NOT NULL,
  dest_topic_id varchar(32) NOT NULL ,
  use_status char(1) NOT NULL,
  login_no varchar(32) NULL,
  opr_time date NULL,
  note varchar(2048) NULL,
  PRIMARY KEY (src_topic_id,attribute_key,attribute_value,dest_topic_id)
);

INSERT INTO tc_topic_map_8 (src_topic_id, attribute_key, attribute_value, dest_topic_id, use_status, login_no, opr_time, note) VALUES
	('TRecOprCntt', '_all', '_default', 'TRecOprCnttDest', '1', 'admin', now(), NULL);

-- 源主题发布关系表
CREATE TABLE tc_topic_pub_8 (
  client_id varchar(32) NOT NULL ,
  src_topic_id varchar(32) NOT NULL ,
  client_pswd char(32) NULL,
  use_status char(1) NOT NULL,
  login_no char(32) NULL,
  opr_time date NULL,
  note varchar(2048) NULL,
  PRIMARY KEY (client_id,src_topic_id)
);

INSERT INTO tc_topic_pub_8 (client_id, src_topic_id, client_pswd, use_status, login_no, opr_time, note) VALUES
	('Pub101', 'TRecOprCntt', '_null', '1', 'admin', now(), NULL);

--