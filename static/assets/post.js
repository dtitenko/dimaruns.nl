const links = [...document.querySelectorAll(".toc .toc-list a")];
const bar = document.querySelector(".toc .progress span");
const body = document.querySelector(".post-body");

if (links.length && body) {
  const headings = links.map((link) => document.getElementById(decodeURIComponent(link.hash.slice(1))));

  const highlight = () => {
    let current = -1;
    headings.forEach((heading, i) => {
      if (heading && heading.getBoundingClientRect().top < window.innerHeight * 0.3) current = i;
    });
    links.forEach((link, i) => {
      if (i === current) link.setAttribute("aria-current", "true");
      else link.removeAttribute("aria-current");
    });
    if (bar) {
      const rect = body.getBoundingClientRect();
      const done = (window.innerHeight * 0.3 - rect.top) / rect.height;
      bar.style.transform = `scaleX(${Math.min(1, Math.max(0, done))})`;
    }
  };

  let queued = false;
  const schedule = () => {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => {
      queued = false;
      highlight();
    });
  };
  window.addEventListener("scroll", schedule, { passive: true });
  window.addEventListener("resize", schedule);
  highlight();
}
