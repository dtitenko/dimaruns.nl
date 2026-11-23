# The zone was created in the dashboard (Cloudflare must hand out nameservers before the
# registrar can delegate). This adopts it into state instead of recreating it.
import {
  to = cloudflare_zone.site
  id = var.zone_id
}

resource "cloudflare_zone" "site" {
  account = { id = var.account_id }
  name    = var.domain
  type    = "full"

  lifecycle {
    prevent_destroy = true
  }
}

locals {
  # HTTPS-only with modern TLS. HSTS and other security headers are set by the edge Worker,
  # next to the CSP, so all response headers are defined in one place.
  zone_settings = {
    ssl                      = "strict"
    always_use_https         = "on"
    automatic_https_rewrites = "on"
    min_tls_version          = "1.2"
    tls_1_3                  = "on"
    http3                    = "on"
  }
}

resource "cloudflare_zone_setting" "site" {
  for_each = local.zone_settings

  zone_id    = cloudflare_zone.site.id
  setting_id = each.key
  value      = each.value
}

resource "cloudflare_zone_dnssec" "site" {
  count = var.enable_dnssec ? 1 : 0

  zone_id = cloudflare_zone.site.id
  status  = "active"

  # Cloudflare reports "pending" until the DS record is published at the registry, then
  # flips to "active" by itself. Without this every plan in between shows a no-op update.
  lifecycle {
    ignore_changes = [status]
  }
}
