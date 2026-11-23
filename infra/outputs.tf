output "name_servers" {
  description = "Set these at the registrar."
  value       = cloudflare_zone.site.name_servers
}

output "zone_status" {
  description = "'pending' until the registrar delegates to Cloudflare, then 'active'."
  value       = cloudflare_zone.site.status
}

output "dnssec_ds_record" {
  description = "Add this DS record at the registrar after enabling DNSSEC."
  value       = one(cloudflare_zone_dnssec.site[*].ds)
}

# IDs needed for the Worker's wrangler configuration (phase 4). Not secret.
output "d1_database_id" {
  value = cloudflare_d1_database.main.id
}

output "kv_namespace_id" {
  value = cloudflare_workers_kv_namespace.cache.id
}

output "r2_media_bucket" {
  value = cloudflare_r2_bucket.media.name
}
