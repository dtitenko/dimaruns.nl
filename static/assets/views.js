const target = document.querySelector("[data-views]");

if (target) {
  fetch(`/api/views/${encodeURIComponent(target.dataset.views)}`, { method: "POST" }).catch(() => {});
}
