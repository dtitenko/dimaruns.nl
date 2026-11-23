// Counts a view of the current post and shows the total. Optional: the page works without it,
// and locally (no /api) the request simply fails and nothing is shown.
const target = document.querySelector('[data-views]');

if (target) {
  fetch(`/api/views/${encodeURIComponent(target.dataset.views)}`, { method: 'POST' })
    .then((response) => (response.ok ? response.json() : null))
    .then((data) => {
      if (data) target.textContent = ` · ${data.views.toLocaleString('en')} views`;
    })
    .catch(() => {});
}
