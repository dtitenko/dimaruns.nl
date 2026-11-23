// Cloudflare Workers entry point. All logic is Kotlin (src/jsMain); this only adapts the module
// shape Workers expects. Run `./gradlew :edge:assembleWorker` first to produce build/worker.
import { handleFetch } from './build/worker/dimaruns-edge.mjs';

export default {
  fetch: (request, env) => handleFetch(request, env),
};
