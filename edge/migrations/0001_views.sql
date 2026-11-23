-- Page view counter per post. Applied with `npm run migrate` (wrangler d1 migrations apply).
CREATE TABLE views (
  slug  TEXT    PRIMARY KEY,
  count INTEGER NOT NULL DEFAULT 0
) STRICT;
