# AWS MVP Policy

## Target Services

Required:
- Spring Boot runtime
- PostgreSQL
- SQS + DLQ
- S3 when remote object storage is needed
- CloudWatch logs
- HTTPS
- environment/secrets management

Not required for MVP:
- Kafka/MSK
- Redis/ElastiCache
- NAT Gateway unless architecture requires it
- complex multi-service deployment

## Minimal Deployment

Ultra-minimal:
- one EC2 instance
- Docker Compose for app + Postgres
- SQS managed by AWS
- CloudWatch agent/log shipping

Preferred MVP:
- EC2 or ECS for Spring Boot
- RDS Postgres
- SQS + DLQ
- S3 for uploads
- CloudWatch logs/alarms
- ALB or reverse proxy with TLS

## Secrets

No secrets in:
- Git
- Expo config
- Docker images
- application default properties

Use:
- environment variables
- AWS Secrets Manager or SSM Parameter Store
- EAS secrets for mobile build-time values

## Cost Guardrails

Avoid:
- MSK
- ElastiCache without critical use
- NAT Gateway for tiny MVP traffic
- excessive CloudWatch retention
- oversized RDS/EC2 instances

