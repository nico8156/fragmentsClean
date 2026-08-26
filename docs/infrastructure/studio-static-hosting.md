# Studio static hosting

The Studio is a Vite/React frontend and can be deployed as static artifacts:

```text
Vite build
  -> private S3 bucket in eu-west-3
  -> CloudFront Origin Access Control
  -> studio-staging.anchor-event.fr
```

## CloudFormation order

The templates are intentionally split by region and lifecycle:

1. `studio-static-bucket.yaml` in `eu-west-3`;
2. `studio-certificate.yaml` in `us-east-1`;
3. retrieve the ACM DNS validation CNAME and create it in the OVH DNS zone;
4. `studio-cloudfront.yaml` in `us-east-1`;
5. create the `studio-staging` CNAME to the CloudFront distribution in OVH;
6. build and sync `fragments-admin/fragments-studio/dist` to the bucket;
7. invalidate CloudFront after a release.

CloudFront requires the viewer ACM certificate in `us-east-1`. The site bucket
and its content remain in `eu-west-3`.

## Security model

- S3 public access block remains enabled;
- CloudFront uses Origin Access Control with SigV4;
- the bucket policy permits only `s3:GetObject` from this distribution;
- the distribution redirects HTTP to HTTPS;
- no AWS credential or admin token enters the Studio bundle;
- Vite production validation requires OAuth mode and HTTPS backend URL.

## SPA routing

CloudFront maps 403/404 responses to `/index.html` for browser routes. API
traffic does not pass through this distribution; Studio calls the Fragments
backend host configured by `VITE_FRAGMENTS_BACKEND_URL`.

## OVH DNS and ACM validation

The authoritative DNS zone is managed by OVH, not Route 53. The certificate
stack therefore requests a DNS-validated ACM certificate without attempting to
modify DNS. The validation CNAME must be copied from ACM and created in OVH.

Use the following controlled sequence from a machine authenticated to AWS:

```bash
aws cloudformation deploy \
  --region us-east-1 \
  --stack-name fragments-studio-certificate-staging \
  --template-file infra/aws/cloudformation/studio-certificate.yaml

CERT_ARN=$(aws cloudformation describe-stacks \
  --region us-east-1 \
  --stack-name fragments-studio-certificate-staging \
  --query 'Stacks[0].Outputs[?OutputKey==`StudioCertificateArn`].OutputValue' \
  --output text)

aws acm describe-certificate \
  --region us-east-1 \
  --certificate-arn "$CERT_ARN" \
  --query 'Certificate.DomainValidationOptions[].ResourceRecord'
```

Create the returned `Name` and `Value` as a CNAME in the OVH zone. After DNS
propagation, wait for `ISSUED`:

```bash
aws acm wait certificate-validated \
  --region us-east-1 \
  --certificate-arn "$CERT_ARN"
```

The CloudFront CNAME is created only after the distribution exists. It must
point `studio-staging` to the distribution hostname, not to the ACM validation
record. No OVH API token or AWS credential belongs in Git, the Studio bundle,
or the conversation.

This phase creates infrastructure templates and the controlled OVH procedure;
it does not create AWS resources, DNS records, or upload a Studio build.
