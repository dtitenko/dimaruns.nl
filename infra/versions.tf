terraform {
  required_version = "~> 1.13"

  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 5.29"
    }
  }

  # State lives in Cloudflare R2 through its S3-compatible API. The bucket is bootstrapped once
  # outside OpenTofu (see infra/README.md). Credentials come from AWS_ACCESS_KEY_ID /
  # AWS_SECRET_ACCESS_KEY and the endpoint from AWS_ENDPOINT_URL_S3, so nothing secret is in code.
  backend "s3" {
    bucket = "dimaruns-tofu-state"
    key    = "prod/terraform.tfstate"
    region = "auto"

    # R2 is not AWS: skip AWS-only checks.
    skip_credentials_validation = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    skip_metadata_api_check     = true
    skip_s3_checksum            = true
    use_path_style              = true

    # Native S3 locking via conditional writes, so concurrent applies cannot corrupt state.
    use_lockfile = true
  }
}
