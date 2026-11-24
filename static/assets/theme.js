// Colour theme switcher. Loaded as a classic script in <head> so the saved theme applies before first paint
// (no flash of the default theme); an external file rather than inline so the strict CSP in _headers holds.
const THEMES = ["dark", "light", "hacker", "solarized", "kimbie"];
const root = document.documentElement;

function read() {
  try {
    return localStorage.getItem("theme");
  } catch {
    return null; // storage blocked: stay on the default theme
  }
}

function apply(theme) {
  if (!THEMES.includes(theme)) theme = "dark";
  if (theme === "dark") delete root.dataset.theme;
  else root.dataset.theme = theme;
  for (const button of document.querySelectorAll("[data-set-theme]")) {
    button.setAttribute("aria-pressed", String(button.dataset.setTheme === theme));
  }
  return theme;
}

apply(read());

document.addEventListener("DOMContentLoaded", () => {
  const switcher = document.querySelector(".theme-switcher");
  if (!switcher) return;
  const toggle = switcher.querySelector(":scope > button");
  apply(read());

  toggle.addEventListener("click", () => {
    const open = switcher.classList.toggle("open");
    toggle.setAttribute("aria-expanded", String(open));
  });
  switcher.addEventListener("click", (event) => {
    const button = event.target.closest("[data-set-theme]");
    if (!button) return;
    const theme = apply(button.dataset.setTheme);
    try {
      localStorage.setItem("theme", theme);
    } catch {
      // storage blocked: the theme still applies to this page
    }
    switcher.classList.remove("open");
    toggle.setAttribute("aria-expanded", "false");
  });
  document.addEventListener("click", (event) => {
    if (!switcher.contains(event.target)) {
      switcher.classList.remove("open");
      toggle.setAttribute("aria-expanded", "false");
    }
  });
});
