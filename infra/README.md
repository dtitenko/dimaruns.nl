# Infrastructure

Cloudflare resources for dimaruns.nl, managed with [OpenTofu](https://opentofu.org). The OpenTofu version is pinned in `.opentofu-version`; provider versions are pinned in `.terraform.lock.hcl`.

| File | Manages |
|---|---|
| `zone.tf` | The `dimaruns.nl` zone (adopted with an `import` block), TLS/HTTPS settings, optional DNSSEC |
| `dns.tf` | DNS records. For now only records saying the domain sends no email (null MX, SPF, DMARC, DKIM) |
| `storage.tf` | D1 database, KV namespace and R2 media bucket used by the edge Worker |

## Running locally

```sh
cp .env.example .env    # fill in values (see below)
scripts/tofu.sh init
scripts/tofu.sh plan
scripts/tofu.sh apply
```

`scripts/tofu.sh` downloads the pinned OpenTofu into `.tools/` (checksum-verified), loads `.env` and runs inside `infra/`.

## State

State is stored in the R2 bucket `dimaruns-tofu-state` through the S3-compatible API, with lock-file locking. The bucket was created once with the Cloudflare API, because OpenTofu needs it to exist before it can store state:

```sh
curl -X POST "https://api.cloudflare.com/client/v4/accounts/$CLOUDFLARE_ACCOUNT_ID/r2/buckets" \
  -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"dimaruns-tofu-state","locationHint":"weur"}'
```

S3 credentials are derived from the API token: the access key ID is the token's ID and the secret is the SHA-256 of the token value. Re-derive them whenever the token is rotated.

## CI

`.github/workflows/infra.yml` does the following:

- **Pull requests:** `fmt -check`, `validate`, then `plan`. The plan is posted as a PR comment and updated in place.
- **Merges to `main`:** `apply` in the `production` environment, which requires approval.

Required repository settings:

| Kind | Name | Value |
|---|---|---|
| Variable | `CLOUDFLARE_ACCOUNT_ID` | Account ID |
| Secret | `CLOUDFLARE_API_TOKEN` | API token |
| Secret | `R2_STATE_ACCESS_KEY_ID` | Token ID |
| Secret | `R2_STATE_SECRET_ACCESS_KEY` | SHA-256 of the token |
| Environment | `production` | With required reviewers |

## DNSSEC

DNSSEC is on (`enable_dnssec = true`). Cloudflare signs the zone, and the registrar publishes the key at the `.nl` registry. The zone stays `pending` until the registry has it.

- `scripts/tofu.sh output dnssec_ds_record` prints the DS record.
- `.nl` registrars often ask for the DNSKEY instead: flags `257`, protocol `3`, algorithm `13`, and the `public_key` from `scripts/tofu.sh state show 'cloudflare_zone_dnssec.site[0]'`.

To turn DNSSEC off, **first** remove the key at the registrar and wait for the DS record's TTL (1 hour). Only then set `enable_dnssec = false`. In the other order, validating resolvers treat the domain as broken.
