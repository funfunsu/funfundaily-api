alter table checkin_record add column  task_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务时间';
