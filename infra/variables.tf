variable "account_id" {
  description = "Cloudflare account ID (set via TF_VAR_account_id)."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{32}$", var.account_id))
    error_message = "account_id must be a 32-character hex string."
  }
}

variable "domain" {
  description = "Apex domain of the site."
  type        = string
  default     = "dimaruns.nl"
}

variable "zone_id" {
  description = "ID of the zone created in the dashboard; adopted into state with an import block."
  type        = string
  default     = "9e8238598c845701de207a676e4be216"
}

variable "enable_dnssec" {
  description = <<-EOT
    Turn on DNSSEC. Enable only once the domain is active on Cloudflare, then copy the
    `dnssec_ds_record` output to the registrar.
  EOT
  type        = bool
  default     = true
}
