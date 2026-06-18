const root = document.documentElement;
document.body.classList.remove("no-js");
document.body.classList.add("js");

const themeToggle = document.querySelector(".theme-toggle");
const menuButton = document.querySelector(".menu-button");
const mobileNav = document.querySelector(".mobile-nav");
const header = document.querySelector(".site-header");

const savedTheme = localStorage.getItem("qf-theme");
const systemPrefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
root.dataset.theme = savedTheme || (systemPrefersDark ? "dark" : "light");

const updateThemeLabel = () => {
  const isDark = root.dataset.theme === "dark";
  themeToggle.setAttribute("aria-label", isDark ? "Ativar tema claro" : "Ativar tema escuro");
  themeToggle.setAttribute("title", isDark ? "Ativar tema claro" : "Ativar tema escuro");
};

const closeMenu = () => {
  mobileNav.classList.remove("open");
  menuButton.setAttribute("aria-expanded", "false");
  menuButton.setAttribute("aria-label", "Abrir menu");
};

updateThemeLabel();

themeToggle.addEventListener("click", () => {
  const nextTheme = root.dataset.theme === "dark" ? "light" : "dark";
  root.dataset.theme = nextTheme;
  localStorage.setItem("qf-theme", nextTheme);
  updateThemeLabel();
});

menuButton.addEventListener("click", () => {
  const isOpen = mobileNav.classList.toggle("open");
  menuButton.setAttribute("aria-expanded", isOpen);
  menuButton.setAttribute("aria-label", isOpen ? "Fechar menu" : "Abrir menu");
});

mobileNav.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", closeMenu);
});

const updateHeader = () => {
  header.classList.toggle("scrolled", window.scrollY > 20);
};

updateHeader();
window.addEventListener("scroll", updateHeader, { passive: true });

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && mobileNav.classList.contains("open")) {
    closeMenu();
    menuButton.focus();
  }
});

window.addEventListener("resize", () => {
  if (window.innerWidth >= 768 && mobileNav.classList.contains("open")) closeMenu();
}, { passive: true });

const animatedItems = document.querySelectorAll(".fade-up");

if ("IntersectionObserver" in window && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.08, rootMargin: "0px 0px -32px" });

  animatedItems.forEach((item) => observer.observe(item));
} else {
  animatedItems.forEach((item) => item.classList.add("visible"));
}

const cardStack = document.querySelector(".app-card-stack");
const appCards = cardStack ? [...cardStack.querySelectorAll(".app-screen-card")] : [];
const carouselDots = [...document.querySelectorAll(".carousel-dot")];
const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
let activeCard = 0;
let cardTimer;
let cardStackHovered = false;
let cardStackFocused = false;
let touchStartX = null;
let touchMoved = false;

const renderCardStack = () => {
  appCards.forEach((card, index) => {
    const position = (index - activeCard + appCards.length) % appCards.length;
    card.classList.remove("card-front", "card-right", "card-left");
    card.classList.add(position === 0 ? "card-front" : position === 1 ? "card-left" : "card-right");
    card.setAttribute("aria-hidden", position === 0 ? "false" : "true");
  });

  if (cardStack) {
    const activeImage = appCards[activeCard]?.querySelector("img");
    cardStack.setAttribute("aria-label", `${activeImage?.alt || "Tela do aplicativo"}. Ative para ver a próxima tela.`);
  }

  carouselDots.forEach((dot, index) => {
    const isActive = index === activeCard;
    dot.classList.toggle("is-active", isActive);
    dot.setAttribute("aria-current", String(isActive));
  });
};

const showCard = (index) => {
  if (!appCards.length) return;
  activeCard = (index + appCards.length) % appCards.length;
  renderCardStack();
};

const showNextCard = () => {
  showCard(activeCard + 1);
};

const showPreviousCard = () => {
  showCard(activeCard - 1);
};

const stopCardRotation = () => {
  window.clearInterval(cardTimer);
};

const startCardRotation = () => {
  stopCardRotation();
  if (!reducedMotion.matches && !cardStackHovered && !cardStackFocused && appCards.length > 1) {
    cardTimer = window.setInterval(showNextCard, 3000);
  }
};

if (cardStack && appCards.length) {
  renderCardStack();
  startCardRotation();

  cardStack.addEventListener("click", () => {
    if (touchMoved) {
      touchMoved = false;
      return;
    }
    showNextCard();
    startCardRotation();
  });
  cardStack.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      showNextCard();
      startCardRotation();
    }
  });
  cardStack.addEventListener("pointerenter", (event) => {
    if (event.pointerType === "mouse") {
      cardStackHovered = true;
      stopCardRotation();
    }
  });
  cardStack.addEventListener("pointerleave", (event) => {
    if (event.pointerType === "mouse") {
      cardStackHovered = false;
      startCardRotation();
    }
  });
  cardStack.addEventListener("focusin", () => {
    cardStackFocused = true;
    stopCardRotation();
  });
  cardStack.addEventListener("focusout", () => {
    cardStackFocused = false;
    startCardRotation();
  });
  cardStack.addEventListener("pointerdown", (event) => {
    if (event.pointerType !== "mouse") {
      touchStartX = event.clientX;
      touchMoved = false;
      stopCardRotation();
    }
  });
  cardStack.addEventListener("pointerup", (event) => {
    if (touchStartX === null || event.pointerType === "mouse") return;
    const distance = event.clientX - touchStartX;
    touchStartX = null;

    if (Math.abs(distance) >= 42) {
      touchMoved = true;
      if (distance < 0) showNextCard();
      else showPreviousCard();
    }

    startCardRotation();
  });
  cardStack.addEventListener("pointercancel", () => {
    touchStartX = null;
    touchMoved = false;
    startCardRotation();
  });
  reducedMotion.addEventListener("change", startCardRotation);

  carouselDots.forEach((dot, index) => {
    dot.addEventListener("click", () => {
      showCard(index);
      startCardRotation();
    });
  });
}

const faqButtons = [...document.querySelectorAll(".faq-question")];

faqButtons.forEach((button) => {
  const toggleFaqItem = () => {
    const item = button.closest(".faq-item");
    const answer = document.getElementById(button.getAttribute("aria-controls"));
    const willOpen = button.getAttribute("aria-expanded") !== "true";

    faqButtons.forEach((otherButton) => {
      const otherItem = otherButton.closest(".faq-item");
      const otherAnswer = document.getElementById(otherButton.getAttribute("aria-controls"));
      otherButton.setAttribute("aria-expanded", "false");
      otherItem.classList.remove("is-open");
      otherAnswer.hidden = true;
    });

    if (willOpen) {
      button.setAttribute("aria-expanded", "true");
      item.classList.add("is-open");
      answer.hidden = false;
    }
  };

  button.addEventListener("click", toggleFaqItem);
  button.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      toggleFaqItem();
    }
});

// --- Integração com API Pública ---
document.addEventListener("DOMContentLoaded", () => {
  if (window.PublicApi) {
    const totalEventsEl = document.getElementById("stats-total-events");
    const avatarCountEl = document.getElementById("stats-avatar-count");

    // Usa IntersectionObserver para carregar apenas quando a seção estiver visível ou próxima
    const heroSection = document.querySelector(".hero");
    if (heroSection && "IntersectionObserver" in window) {
      const apiObserver = new IntersectionObserver(async (entries) => {
        if (entries[0].isIntersecting) {
          apiObserver.disconnect();
          loadStats(totalEventsEl, avatarCountEl);
        }
      }, { rootMargin: "200px" });
      apiObserver.observe(heroSection);
    } else {
      loadStats(totalEventsEl, avatarCountEl);
    }
  }
});

async function loadStats(totalEventsEl, avatarCountEl) {
  const stats = await window.PublicApi.getStats();
  if (stats && typeof stats.totalEvents === 'number') {
    if (totalEventsEl) {
      const text = window.PublicApi.formatStatsText(stats.totalEvents);
      totalEventsEl.textContent = `${text} momentos`;
    }
    if (avatarCountEl && stats.totalUsers > 0) {
      // Ex: 734 users -> +700
      let userCountText = `+${stats.totalUsers}`;
      if (stats.totalUsers >= 1000) {
        userCountText = `+${Math.floor(stats.totalUsers / 1000)}k`;
      }
      avatarCountEl.textContent = userCountText;
    }
  }
}
