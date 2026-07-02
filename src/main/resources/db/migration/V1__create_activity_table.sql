create table activity
(
    id bigserial primary key,
    fit_id bigint not null unique,
    sport varchar(50),
    average_heart_rate smallint,
    aerobic_training_effect real,
    anaerobic_training_effect real,
    started_at timestamp not null
);