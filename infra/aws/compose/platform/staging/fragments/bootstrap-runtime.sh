#!/usr/bin/env bash
set -Eeuo pipefail

runtime_root=/srv/fragments/staging
aws_region=eu-west-3
backend_image=${1:?"Usage: bootstrap-runtime.sh <backend-image>"}

runtime_pg_password=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/POSTGRES_PASSWORD --with-decryption --query Parameter.Value --output text)
runtime_jwt_secret=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/AUTH_JWT_SECRET --with-decryption --query Parameter.Value --output text)
runtime_google_places_key=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/GOOGLE_PLACES_API_KEY --with-decryption --query Parameter.Value --output text)
runtime_google_studio_secret=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/GOOGLE_STUDIO_CLIENT_SECRET --with-decryption --query Parameter.Value --output text)
runtime_openai_key=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/OPENAI_API_KEY --with-decryption --query Parameter.Value --output text)
runtime_openai_project_id=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/OPENAI_PROJECT_ID --with-decryption --query Parameter.Value --output text 2>/dev/null || true)
# This is deliberately a normal SSM String, not a secret: it identifies the
# operator account allowed to bootstrap an otherwise empty admin allow-list.
runtime_admin_bootstrap_user_ids=$(aws ssm get-parameter --region "$aws_region" --name /fragments/staging/ADMIN_SECURITY_BOOTSTRAP_USER_IDS --query Parameter.Value --output text 2>/dev/null || true)

umask 077
: > "$runtime_root/.env"
write_env() { printf '%s=%s\n' "$1" "$2" >> "$runtime_root/.env"; }

write_env BACKEND_IMAGE "$backend_image"
write_env POSTGRES_USER fragments
write_env POSTGRES_DB fragmentsclean
write_env POSTGRES_PASSWORD "$runtime_pg_password"
write_env AUTH_JWT_SECRET "$runtime_jwt_secret"
write_env GOOGLE_PLACES_API_KEY "$runtime_google_places_key"
write_env OPENAI_API_KEY "$runtime_openai_key"
write_env OPENAI_PROJECT_ID "$runtime_openai_project_id"
write_env ARTICLE_GENERATION_OPENAI_ENABLED true
write_env ARTICLE_GENERATION_TEXT_MODEL gpt-4o-mini
write_env ARTICLE_GENERATION_IMAGE_MODEL gpt-image-1-mini
write_env ARTICLE_GENERATION_SCHEDULE_ENABLED true
write_env ARTICLE_GENERATION_SCHEDULE_DELAY_MS 604800000
write_env ARTICLE_GENERATION_SCHEDULE_SUBJECT "Découverte hebdomadaire autour de la culture café"
write_env ARTICLE_GENERATION_SCHEDULE_LOCALE fr-FR
write_env ARTICLE_GENERATION_SCHEDULE_MAX_PENDING 2
write_env ARTICLE_GENERATION_SCHEDULE_DEDUPLICATION_HOURS 168
write_env SPRING_PROFILES_ACTIVE prod
write_env AWS_REGION "$aws_region"
write_env FRAGMENTS_EDGE_NETWORK fragments-staging-edge
write_env GOOGLE_MOBILE_IOS_CLIENT_ID 255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe.apps.googleusercontent.com
write_env GOOGLE_MOBILE_IOS_REDIRECT_URI com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect
write_env GOOGLE_STUDIO_CLIENT_ID 255942605258-1nji47405hqf1q2imk35toejorv1opsk.apps.googleusercontent.com
write_env GOOGLE_STUDIO_REDIRECT_URI https://studio-staging.anchor-event.fr/
write_env GOOGLE_STUDIO_CLIENT_SECRET "$runtime_google_studio_secret"
write_env ADMIN_SECURITY_BOOTSTRAP_USER_IDS "$runtime_admin_bootstrap_user_ids"
write_env AUTH_JWT_ISSUER https://auth.fragments
write_env FRAGMENTS_CORS_ALLOWED_ORIGINS https://studio-staging.anchor-event.fr
write_env APP_MESSAGING_LOCAL_EVENT_BUS_ENABLED false
write_env APP_MESSAGING_SQS_ENABLED true
write_env SQS_ARTICLES_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-articles-events
write_env SQS_AUTH_USERS_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-auth-users-events
write_env SQS_APP_USERS_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-app-users-events
write_env SQS_COFFEES_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-coffees-events
write_env SQS_DOMAIN_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-domain-events
write_env SQS_TICKET_EVENTS_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-ticket-events
write_env SQS_TICKET_VERIFICATION_REQUESTED_URL https://sqs.eu-west-3.amazonaws.com/851725375299/fragments-staging-ticket-verification-requested
write_env COFFEE_PHOTOS_STORAGE_BACKEND s3
write_env COFFEE_PHOTOS_S3_BUCKET anchor-assets-prod-851725375299
write_env COFFEE_PHOTOS_S3_PREFIX fragments/staging/coffees
write_env COFFEE_PHOTOS_S3_REGION "$aws_region"
write_env COFFEE_PHOTOS_PUBLIC_BASE_URL https://fragments-staging.anchor-event.fr
write_env ARTICLE_IMAGES_STORAGE_BACKEND s3
write_env ARTICLE_IMAGES_S3_BUCKET anchor-assets-prod-851725375299
write_env ARTICLE_IMAGES_S3_PREFIX fragments/staging/articles
write_env ARTICLE_IMAGES_S3_REGION "$aws_region"
write_env ARTICLE_IMAGES_PUBLIC_BASE_URL https://fragments-staging.anchor-event.fr

unset runtime_pg_password runtime_jwt_secret runtime_google_places_key runtime_google_studio_secret runtime_openai_key runtime_openai_project_id runtime_admin_bootstrap_user_ids
