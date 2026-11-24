const THEMES = { system: "System", light: "Light", dark: "Dark" };
const root = document.documentElement;

function read() {
  try {
    return localStorage.getItem("theme");
  } catch {
    return null;
  }
}

function apply(theme) {
  if (!(theme in THEMES)) theme = "system";
  if (theme === "system") delete root.dataset.theme;
  else root.dataset.theme = theme;
  for (const button of document.querySelectorAll("[data-set-theme]")) {
    button.setAttribute("aria-pressed", String(button.dataset.setTheme === theme));
  }
  document.querySelector(".theme-switcher > button")?.setAttribute("aria-label", `Theme: ${THEMES[theme]}`);
  return theme;
}

apply(read());

document.addEventListener("DOMContentLoaded", () => {
  const switcher = document.querySelector(".theme-switcher");
  if (!switcher) return;
  const toggle = switcher.querySelector(":scope > button");
  apply(read());

  const setOpen = (open) => {
    switcher.classList.toggle("open", open);
    toggle.setAttribute("aria-expanded", String(open));
  };

  toggle.addEventListener("click", () => setOpen(!switcher.classList.contains("open")));
  switcher.addEventListener("click", (event) => {
    const button = event.target.closest("[data-set-theme]");
    if (!button) return;
    const theme = apply(button.dataset.setTheme);
    try {
      localStorage.setItem("theme", theme);
    } catch {
      // storage blocked: the theme still applies to this page
    }
    setOpen(false);
    toggle.focus();
  });
  switcher.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && switcher.classList.contains("open")) {
      setOpen(false);
      toggle.focus();
    }
  });
  document.addEventListener("click", (event) => {
    if (!switcher.contains(event.target)) setOpen(false);
  });
});
