-- Move Pass ownership to userApplicationContext using event-fed local contributions.
create table if not exists ticket_submission_fingerprints (
    fingerprint varchar(64) primary key, ticket_id uuid not null unique,
    user_id uuid not null, created_at timestamptz not null
);
insert into ticket_submission_fingerprints(fingerprint,ticket_id,user_id,created_at)
select fingerprint,ticket_id,user_id,created_at from (
    select 'v1:' || md5(lower(regexp_replace(btrim(ocr_text), '\s+', ' ', 'g'))) fingerprint,
           ticket_id,user_id,created_at,
           row_number() over (partition by md5(lower(regexp_replace(btrim(ocr_text), '\s+', ' ', 'g')))
                              order by created_at,ticket_id) ordinal
    from tickets where ocr_text is not null and btrim(ocr_text) <> ''
) canonical where ordinal=1 on conflict do nothing;

create table if not exists pass_ticket_contributions (
    ticket_id uuid primary key, user_id uuid not null, active boolean not null,
    source_version bigint not null, updated_at timestamptz not null
);
create index if not exists idx_pass_ticket_contributions_user_active
    on pass_ticket_contributions(user_id, active);
create table if not exists pass_experience_contributions (
    experience_id uuid primary key, user_id uuid not null, coffee_id uuid not null,
    active boolean not null, source_version bigint not null, updated_at timestamptz not null
);
create index if not exists idx_pass_experience_contributions_user_active
    on pass_experience_contributions(user_id, active);
create table if not exists user_pass_projection (
    user_id uuid primary key, policy_version integer not null,
    published_experiences integer not null, distinct_experienced_coffees integer not null,
    validated_tickets integer not null, acquired_levels text not null,
    version bigint not null, updated_at timestamptz not null
);

insert into pass_ticket_contributions(ticket_id,user_id,active,source_version,updated_at)
select ticket_id,user_id,(status='CONFIRMED'),version,updated_at from tickets
where ocr_text is null or btrim(ocr_text) = ''
   or exists (select 1 from ticket_submission_fingerprints fingerprint
              where fingerprint.ticket_id=tickets.ticket_id)
on conflict(ticket_id) do nothing;

with legacy as (
    select contribution.user_id,
           count(*) filter (where contribution.active) :: integer as tickets,
           (select count(*) from social_comments_projection comment
             where comment.author_id=contribution.user_id and comment.deleted_at is null
               and comment.moderation='PUBLISHED') :: integer as comments,
           (select count(*) from social_likes_projection liked
             where liked.user_id=contribution.user_id and liked.active=true) :: integer as likes,
           max(contribution.updated_at) as updated_at
    from pass_ticket_contributions contribution group by contribution.user_id
)
insert into user_pass_projection(user_id,policy_version,published_experiences,
    distinct_experienced_coffees,validated_tickets,acquired_levels,version,updated_at)
select user_id,2,0,0,tickets,
       case when tickets>=10 and comments>=5 and likes>=5
              then 'COFFEE_TASTER,URBAN_EXPLORER,SOCIAL_BEAN,FRAGMENTS_MASTER'
            when tickets>=5 and comments>=3 then 'COFFEE_TASTER,URBAN_EXPLORER'
            when tickets>=3 then 'COFFEE_TASTER' else '' end,
       1,updated_at
from legacy on conflict(user_id) do nothing;
