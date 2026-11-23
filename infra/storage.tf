# Data stores used by the edge Worker. prevent_destroy guards against losing data through a
# refactor or a bad plan; removing one requires an explicit code change first.

# View counters and webmentions.
resource "cloudflare_d1_database" "main" {
  account_id            = var.account_id
  name                  = "dimaruns"
  primary_location_hint = "weur"

  read_replication = {
    mode = "disabled"
  }

  lifecycle {
    prevent_destroy = true
  }
}

# Search index and cached responses.
resource "cloudflare_workers_kv_namespace" "cache" {
  account_id = var.account_id
  title      = "dimaruns-cache"
}

# Images and other media too large for the repository.
resource "cloudflare_r2_bucket" "media" {
  account_id    = var.account_id
  name          = "dimaruns-media"
  location      = "weur"
  storage_class = "Standard"

  lifecycle {
    prevent_destroy = true
  }
}
