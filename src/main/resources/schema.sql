DROP TABLE coffees;

create table coffees (
    id UUID primary key,
    google_id varchar(255) not null,
    display_name varchar(255) not null,
    formatted_address varchar(255) not null,
    national_phone_number varchar(255),
    website_uri varchar(255),
    latitude double precision not null,
    longitude double precision not null
);