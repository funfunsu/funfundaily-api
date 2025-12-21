
alter table schedule_item drop key schedule_item_idx;



alter table schedule_item add
    KEY `schedule_user_idx` (`user_id`,`item_type`,`repeat_start_day`,`repeat_end_day`);

alter table schedule_item add
    KEY `schedule_group_idx` (`group_id`,`item_type`,`repeat_start_day`,`repeat_end_day`);


alter table schedule_item add
    KEY `schedule_time_idx` (`repeat_start_day`,`repeat_end_day`);