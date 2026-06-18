/* =============================================
   QUANTO FALTA? — app.js v2
   Performance-optimized, accessible, robust
   ============================================= */

document.addEventListener('DOMContentLoaded', () => {

  const PUBLIC_API_BASE = 'https://api.tocontando.com.br/api/v1/public';

  // ── Respect reduced motion preference ─────────────────────────────────
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  document.querySelectorAll('img[data-fallback-src]').forEach((img) => {
    img.addEventListener('error', () => {
      const fallback = img.getAttribute('data-fallback-src');
      if (fallback && img.getAttribute('src') !== fallback) {
        img.setAttribute('src', fallback);
      }
    }, { once: true });
  });

  // ── 1. Scroll Reveal (Intersection Observer) ──────────────────────────
  const revealEls = document.querySelectorAll(
    '.feature-card, .showcase-card, .section-header, .cta-card, .faq-item, .premium-card, .widget-card'
  );

  if (!prefersReducedMotion) {
    revealEls.forEach((el, i) => {
      el.classList.add('reveal');
      const delay = Math.min(i % 4, 3);
      if (delay > 0) el.classList.add(`reveal-delay-${delay}`);
    });

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -32px 0px' });

    revealEls.forEach(el => observer.observe(el));
  }


  // ── 2. Animated number tickers on showcase cards ──────────────────────
  if (!prefersReducedMotion) {
    const showcaseNumbers = document.querySelectorAll('.showcase-card-number');

    const numberObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const el = entry.target;
          const final = parseInt(el.textContent, 10);
          if (!isNaN(final)) animateNumber(el, final + 30, final, 800);
          numberObserver.unobserve(el);
        }
      });
    }, { threshold: 0.5 });

    showcaseNumbers.forEach(el => numberObserver.observe(el));
  }

  function animateNumber(el, from, to, duration) {
    const start = performance.now();
    const update = (time) => {
      const progress = Math.min((time - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      el.textContent = Math.round(from + (to - from) * eased);
      if (progress < 1) requestAnimationFrame(update);
    };
    requestAnimationFrame(update);
  }


  // ── 3. Nav background intensity on scroll ─────────────────────────────
  const nav = document.querySelector('.nav');
  if (nav) {
    let scrollTick = false;
    window.addEventListener('scroll', () => {
      if (!scrollTick) {
        requestAnimationFrame(() => {
          nav.style.borderBottomColor = window.scrollY > 30
            ? 'rgba(255,255,255,0.1)'
            : 'rgba(255,255,255,0.06)';
          scrollTick = false;
        });
        scrollTick = true;
      }
    }, { passive: true });
  }


  // ── 4. Share demo copy button ──────────────────────────────────────────
  const copyBtn = document.querySelector('.share-demo-copy');
  if (copyBtn) {
    copyBtn.addEventListener('click', async () => {
      const url = 'https://share.tocontando.com.br/s/xK9pL3mN';
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(url);
        } else {
          // Fallback for older browsers / iOS
          const ta = document.createElement('textarea');
          ta.value = url;
          ta.style.cssText = 'position:fixed;opacity:0;';
          document.body.appendChild(ta);
          ta.focus();
          ta.select();
          document.execCommand('copy');
          document.body.removeChild(ta);
        }
        // Success feedback
        const original = copyBtn.innerHTML;
        copyBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#34C759" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>`;
        copyBtn.setAttribute('aria-label', 'Copiado!');
        setTimeout(() => {
          copyBtn.innerHTML = original;
          copyBtn.setAttribute('aria-label', 'Copiar link de compartilhamento');
        }, 1800);
      } catch (e) {
        console.warn('Clipboard unavailable:', e);
      }
    });
  }


  // ── 5. Smooth scroll for anchor links ─────────────────────────────────
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', e => {
      const href = anchor.getAttribute('href');
      if (href === '#') return;
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        const navH = nav ? nav.offsetHeight : 0;
        const top = target.getBoundingClientRect().top + window.scrollY - navH - 16;
        window.scrollTo({ top, behavior: 'smooth' });
      }
    });
  });


  // ── 6. Button ripple effect ────────────────────────────────────────────
  if (!prefersReducedMotion) {
    // Inject ripple keyframe once
    const style = document.createElement('style');
    style.textContent = '@keyframes rippleAnim { to { transform: scale(2.5); opacity: 0; } }';
    document.head.appendChild(style);

    document.querySelectorAll('.btn-primary').forEach(btn => {
      btn.addEventListener('click', function(e) {
        const ripple = document.createElement('span');
        const rect = this.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        ripple.style.cssText = `
          position:absolute;
          width:${size}px;height:${size}px;
          left:${e.clientX - rect.left - size / 2}px;
          top:${e.clientY - rect.top - size / 2}px;
          background:rgba(255,255,255,0.22);
          border-radius:50%;
          transform:scale(0);
          animation:rippleAnim 0.5s ease-out forwards;
          pointer-events:none;
        `;
        this.appendChild(ripple);
        setTimeout(() => ripple.remove(), 600);
      });
    });
  }


  // ── 7. Parallax on hero blobs (desktop only, throttled) ───────────────
  if (!prefersReducedMotion && window.innerWidth > 900) {
    const blob1 = document.querySelector('.bg-blob-1');
    const blob2 = document.querySelector('.bg-blob-2');
    let rafPending = false;
    let mx = 0, my = 0;

    window.addEventListener('mousemove', (e) => {
      mx = (e.clientX / window.innerWidth - 0.5) * 30;
      my = (e.clientY / window.innerHeight - 0.5) * 30;
      if (!rafPending) {
        rafPending = true;
        requestAnimationFrame(() => {
          if (blob1) blob1.style.transform = `translate(${mx}px, ${my}px)`;
          if (blob2) blob2.style.transform = `translate(${-mx * 0.7}px, ${-my * 0.7}px)`;
          rafPending = false;
        });
      }
    }, { passive: true });
  }


  // ── 8. Interactive Hero Screenshots ───────────────────────────────────
  const screens = document.querySelectorAll('.hero-screen');
  screens.forEach(screen => {
    screen.addEventListener('click', () => {
      if (screen.classList.contains('screen-main')) return;
      const currentMain = document.querySelector('.screen-main');
      const clickedClass = screen.classList.contains('screen-left') ? 'screen-left' : 'screen-right';
      screen.classList.remove(clickedClass);
      screen.classList.add('screen-main');
      currentMain.classList.remove('screen-main');
      currentMain.classList.add(clickedClass);
    });

    // Keyboard accessibility
    screen.setAttribute('role', 'button');
    screen.setAttribute('tabindex', screen.classList.contains('screen-main') ? '-1' : '0');
    screen.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        screen.click();
      }
    });
  });


  // ── 9. Fetch Live Stats ───────────────────────────────────────────────
  const statsCountEl = document.getElementById('user-stats-count');
  if (statsCountEl) {
    fetch(`${PUBLIC_API_BASE}/stats/events/count`, {
      signal: AbortSignal.timeout ? AbortSignal.timeout(5000) : undefined,
    })
      .then(res => { if (!res.ok) throw new Error('HTTP ' + res.status); return res.json(); })
      .then(data => {
        const total = data?.data?.total ?? data?.total;
        if (typeof total === 'number') {
          const displayCount = total > 0 ? total + 1500 : 1500;
          statsCountEl.textContent = `+${displayCount.toLocaleString('pt-BR')}`;
        }
      })
      .catch(() => {
        // Silently fail — shows static fallback text
      });
  }


  // ── 10. Fetch public plans / premium price state ───────────────────────
  const premiumPriceBox = document.getElementById('premium-price');
  const premiumPriceValue = document.getElementById('premium-price-value');
  const premiumPricePeriod = document.getElementById('premium-price-period');
  const freeEventLimit = document.getElementById('free-event-limit');

  if (premiumPriceBox && premiumPriceValue && premiumPricePeriod) {
    fetch(`${PUBLIC_API_BASE}/plans`, {
      signal: AbortSignal.timeout ? AbortSignal.timeout(5000) : undefined,
    })
      .then(res => { if (!res.ok) throw new Error('HTTP ' + res.status); return res.json(); })
      .then(payload => {
        const plans = payload?.data?.plans ?? payload?.plans ?? [];
        const freePlan = plans.find(plan => plan.id === 'free' || plan.premium === false);
        const premiumPlan = plans.find(plan => plan.id === 'premium_lifetime' || plan.premium === true);

        if (freePlan?.eventLimit && freeEventLimit) {
          freeEventLimit.textContent = `Até ${freePlan.eventLimit} eventos ativos`;
        }

        const price = premiumPlan?.price ?? premiumPlan?.localizedPrice ?? premiumPlan?.priceFormatted;
        const currency = premiumPlan?.currency ?? premiumPlan?.priceCurrency;

        premiumPriceBox.classList.remove('plan-price-loading', 'plan-price-error', 'plan-price-fallback');

        if (price) {
          premiumPriceValue.textContent = formatPlanPrice(price, currency);
          premiumPricePeriod.textContent = 'pagamento único';
        } else {
          premiumPriceBox.classList.add('plan-price-fallback');
          premiumPriceValue.textContent = 'Preço na Google Play';
          premiumPricePeriod.textContent = 'compra única, sem assinatura';
        }
      })
      .catch(() => {
        premiumPriceBox.classList.remove('plan-price-loading', 'plan-price-fallback');
        premiumPriceBox.classList.add('plan-price-error');
        premiumPriceValue.textContent = 'Preço indisponível';
        premiumPricePeriod.textContent = 'confira na Google Play';
      });
  }

  function formatPlanPrice(price, currency) {
    if (typeof price === 'string') return price;
    if (typeof price === 'number') {
      try {
        return new Intl.NumberFormat('pt-BR', {
          style: 'currency',
          currency: currency || 'BRL',
        }).format(price);
      } catch {
        return String(price);
      }
    }
    return 'Preço na Google Play';
  }


  // ── 11. FAQ keyboard navigation ────────────────────────────────────────
  document.querySelectorAll('.faq-question').forEach(q => {
    q.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        q.closest('details').toggleAttribute('open');
      }
    });
  });

});
