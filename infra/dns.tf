# The domain does not send or receive email. These records say so explicitly, which stops
# spammers from spoofing @dimaruns.nl (receivers reject mail that fails SPF/DMARC).
# Remove them if a mail provider is added later.
locals {
  no_email_records = {
    null_mx = { name = "@", type = "MX", content = ".", priority = 0 } # RFC 7505 "null MX"
    spf     = { name = "@", type = "TXT", content = "\"v=spf1 -all\"" }
    dmarc   = { name = "_dmarc", type = "TXT", content = "\"v=DMARC1; p=reject; sp=reject; adkim=s; aspf=s\"" }
    dkim    = { name = "*._domainkey", type = "TXT", content = "\"v=DKIM1; p=\"" }
  }
}

resource "cloudflare_dns_record" "no_email" {
  for_each = local.no_email_records

  zone_id  = cloudflare_zone.site.id
  name     = each.value.name == "@" ? var.domain : "${each.value.name}.${var.domain}"
  type     = each.value.type
  content  = each.value.content
  priority = try(each.value.priority, null)
  ttl      = 1 # automatic
  comment  = "Managed by OpenTofu: domain sends no email"
}

# Records for the site itself are created in phase 4, when the Worker is attached to the
# domain as a custom domain (Cloudflare then manages the DNS record and certificate).
