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
3. add/verify the Route 53 record for the certificate validation if needed;
4. `studio-cloudfront.yaml` in `us-east-1`;
5. add the Route 53 alias from `studio-staging.anchor-event.fr` to the
   CloudFront distribution;
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

## Current boundary

This phase creates infrastructure templates only. It does not create the S3
bucket, certificate, distribution, DNS record, or upload any Studio build.
