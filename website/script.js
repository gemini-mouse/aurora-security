/* =====================================================================
   AURORA SECURITY - Main JavaScript
   Features: Language Redirect, Aurora Canvas, Theme, Header, App Tour,
   FAQ, Scroll Reveal
   ===================================================================== */

(function redirectRootToPreferredLanguage() {
    const supportedLanguages = {
        en: '/',
        'zh-tw': '/zh-tw/',
        es: '/es/',
        ja: '/ja/',
        ko: '/ko/',
    };

    const rootPaths = ['/', '/index.html'];
    const isRootPage = rootPaths.includes(window.location.pathname);

    if (!isRootPage) {
        return;
    }

    const savedLanguage = localStorage.getItem('aurora-language');
    const preferredLanguage = savedLanguage || detectBrowserLanguage();
    const redirectPath = supportedLanguages[preferredLanguage];

    if (!redirectPath || preferredLanguage === 'en') {
        return;
    }

    window.location.replace(`${redirectPath}${window.location.search}${window.location.hash}`);

    function detectBrowserLanguage() {
        const browserLanguages = navigator.languages?.length
            ? navigator.languages
            : [navigator.language || 'en'];

        for (const language of browserLanguages) {
            const normalized = language.toLowerCase();

            if (normalized.startsWith('zh')) {
                return 'zh-tw';
            }

            if (normalized.startsWith('ja')) {
                return 'ja';
            }

            if (normalized.startsWith('ko')) {
                return 'ko';
            }

            if (normalized.startsWith('es')) {
                return 'es';
            }
        }

        return 'en';
    }
})();

document.addEventListener('DOMContentLoaded', () => {

    /* -----------------------------------------------------------------
       1. Aurora Canvas
       ----------------------------------------------------------------- */
    const canvas = document.getElementById('aurora-canvas');
    const ctx = canvas.getContext('2d');

    function resizeCanvas() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    const isDark = () => document.documentElement.getAttribute('data-theme') !== 'light';

    const bands = [
        { x: 0.15, y: 0.25, w: 0.7, phase: 0, speed: 0.0007, amp: 0.08, hue: 174, alpha: 0.35 },
        { x: 0.0, y: 0.35, w: 1.0, phase: 1.5, speed: 0.0005, amp: 0.06, hue: 270, alpha: 0.25 },
        { x: 0.1, y: 0.18, w: 0.6, phase: 3.0, speed: 0.0009, amp: 0.07, hue: 155, alpha: 0.20 },
        { x: 0.3, y: 0.45, w: 0.5, phase: 0.8, speed: 0.0006, amp: 0.05, hue: 330, alpha: 0.15 },
    ];

    let time = 0;

    function drawAurora() {
        requestAnimationFrame(drawAurora);
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        time++;

        const width = canvas.width;
        const height = canvas.height;

        bands.forEach(band => {
            const yBase = height * band.y + Math.sin(time * band.speed + band.phase) * height * band.amp;
            const gradient = ctx.createLinearGradient(0, yBase - 120, 0, yBase + 120);

            const saturation = isDark() ? '80%' : '65%';
            const lightness = isDark() ? '60%' : '70%';
            const alpha = isDark() ? band.alpha : band.alpha * 0.55;

            gradient.addColorStop(0, `hsla(${band.hue}, ${saturation}, ${lightness}, 0)`);
            gradient.addColorStop(0.3, `hsla(${band.hue}, ${saturation}, ${lightness}, ${alpha * 0.6})`);
            gradient.addColorStop(0.5, `hsla(${band.hue}, ${saturation}, ${lightness}, ${alpha})`);
            gradient.addColorStop(0.7, `hsla(${band.hue}, ${saturation}, ${lightness}, ${alpha * 0.6})`);
            gradient.addColorStop(1, `hsla(${band.hue}, ${saturation}, ${lightness}, 0)`);

            const wavePoints = [];
            const steps = 80;
            const startX = width * band.x;
            const endX = startX + width * band.w;

            for (let i = 0; i <= steps; i++) {
                const t = i / steps;
                const x = startX + t * (endX - startX);
                const wave =
                    Math.sin(t * 3 + time * band.speed * 2 + band.phase) * 60 +
                    Math.sin(t * 7 + time * band.speed + band.phase * 2) * 20 +
                    Math.sin(t * 13 + time * band.speed * 3) * 8;

                wavePoints.push({ x, y: yBase + wave });
            }

            ctx.save();
            ctx.beginPath();
            ctx.moveTo(wavePoints[0].x, wavePoints[0].y - 100);

            for (let i = 1; i < wavePoints.length; i++) {
                const current = wavePoints[i - 1];
                const next = wavePoints[i];
                ctx.quadraticCurveTo(
                    current.x,
                    current.y - 100,
                    (current.x + next.x) / 2,
                    (current.y + next.y) / 2 - 100
                );
            }

            for (let i = wavePoints.length - 1; i >= 1; i--) {
                const current = wavePoints[i];
                const next = wavePoints[i - 1];
                ctx.quadraticCurveTo(
                    current.x,
                    current.y + 100,
                    (current.x + next.x) / 2,
                    (current.y + next.y) / 2 + 100
                );
            }

            ctx.closePath();
            ctx.fillStyle = gradient;
            ctx.filter = 'blur(18px)';
            ctx.fill();
            ctx.filter = 'none';
            ctx.restore();
        });
    }

    drawAurora();

    /* -----------------------------------------------------------------
       2. YouTube Embed
       ----------------------------------------------------------------- */
    document.querySelectorAll('.product-video-iframe[data-youtube-id]').forEach(iframe => {
        const videoId = iframe.dataset.youtubeId;
        if (!videoId) {
            return;
        }

        const origin = window.location.origin && window.location.origin !== 'null'
            ? window.location.origin
            : `${window.location.protocol}//${window.location.host}`;

        const params = new URLSearchParams({
            enablejsapi: '1',
            origin,
            playsinline: '1',
            rel: '0',
            widget_referrer: window.location.href,
        });

        iframe.src = `https://www.youtube.com/embed/${encodeURIComponent(videoId)}?${params.toString()}`;
    });


    /* -----------------------------------------------------------------
       3. Language Dropdown
       ----------------------------------------------------------------- */
    const langBtn = document.getElementById('lang-btn');
    const langMenu = document.getElementById('lang-menu');

    if (langBtn && langMenu) {
        langMenu.querySelectorAll('[data-lang]').forEach(link => {
            link.addEventListener('click', () => {
                localStorage.setItem('aurora-language', link.dataset.lang);
            });
        });

        langBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const expanded = langBtn.getAttribute('aria-expanded') === 'true';
            langBtn.setAttribute('aria-expanded', !expanded);
            langMenu.classList.toggle('show');
        });

        document.addEventListener('click', (e) => {
            if (!langMenu.contains(e.target) && !langBtn.contains(e.target)) {
                langBtn.setAttribute('aria-expanded', 'false');
                langMenu.classList.remove('show');
            }
        });

    }

    /* -----------------------------------------------------------------
       3. Theme Toggle
       ----------------------------------------------------------------- */
    const htmlEl = document.documentElement;
    const themeBtn = document.getElementById('theme-toggle');

    const appTourShots = document.querySelectorAll('.app-tour-shot');

    if (themeBtn) {
        const savedTheme = localStorage.getItem('aurora-theme');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        let currentTheme = savedTheme || (prefersDark ? 'dark' : 'light');

        applyTheme(currentTheme);

        themeBtn.addEventListener('click', () => {
            currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
            applyTheme(currentTheme);
        });
    }

    function applyTheme(theme) {
        htmlEl.setAttribute('data-theme', theme);
        localStorage.setItem('aurora-theme', theme);

        appTourShots.forEach(img => {
            const screen = img.dataset.appScreenImage;
            if (!screen) {
                return;
            }

            const suffix = theme === 'dark' ? '_dark' : '';
            const nextSrc = `assets/screenshot/${screen}${suffix}.png`;
            if (!img.src.endsWith(nextSrc)) {
                img.src = nextSrc;
            }
        });
    }


    /* -----------------------------------------------------------------
       3. Header
       ----------------------------------------------------------------- */
    const header = document.getElementById('site-header');
    let lastScroll = 0;

    if (header) {
        window.addEventListener('scroll', () => {
            const currentY = window.scrollY;
            header.classList.toggle('scrolled', currentY > 20);
            header.classList.toggle('hidden', currentY > lastScroll + 5 && currentY > 200);

            if (currentY < lastScroll) {
                header.classList.remove('hidden');
            }

            lastScroll = currentY;
        }, { passive: true });
    }


    /* -----------------------------------------------------------------
       4. Mobile Navigation
       ----------------------------------------------------------------- */
    const hamburger = document.getElementById('hamburger');
    const mobileNav = document.getElementById('mobile-nav');

    if (hamburger && mobileNav) {
        hamburger.addEventListener('click', () => {
            const isOpen = mobileNav.classList.toggle('open');
            hamburger.classList.toggle('open', isOpen);
            hamburger.setAttribute('aria-label', isOpen ? 'Close menu' : 'Open menu');
        });

        document.querySelectorAll('.mobile-link, .mobile-cta').forEach(link => {
            link.addEventListener('click', () => {
                mobileNav.classList.remove('open');
                hamburger.classList.remove('open');
            });
        });
    }


    /* -----------------------------------------------------------------
       5. FAQ Accordion
       ----------------------------------------------------------------- */
    document.querySelectorAll('.faq-question').forEach(button => {
        button.addEventListener('click', () => {
            const item = button.closest('.faq-item');
            const isActive = item.classList.contains('active');

            document.querySelectorAll('.faq-item').forEach(faqItem => {
                faqItem.classList.remove('active');
                faqItem.querySelector('.faq-question').setAttribute('aria-expanded', 'false');
            });

            if (!isActive) {
                item.classList.add('active');
                button.setAttribute('aria-expanded', 'true');
            }
        });
    });


    /* -----------------------------------------------------------------
       6. App Tour
       ----------------------------------------------------------------- */
    const appTourShell = document.querySelector('.app-tour-shell');

    if (appTourShell) {
        const appTourTabs = Array.from(appTourShell.querySelectorAll('.app-tour-tab'));
        const appTourTargets = appTourShell.querySelectorAll('[data-app-screen-target]');
        const appTourPanels = appTourShell.querySelectorAll('[data-app-screen-panel]');
        const appTourImages = appTourShell.querySelectorAll('[data-app-screen-image]');

        function setAppTourScreen(screen, options = {}) {
            const { focusTab = false } = options;
            appTourShell.dataset.activeScreen = screen;

            appTourTabs.forEach(tab => {
                const isActive = tab.dataset.appScreenTarget === screen;
                tab.classList.toggle('is-active', isActive);
                tab.setAttribute('aria-selected', isActive ? 'true' : 'false');
                tab.tabIndex = isActive ? 0 : -1;

                if (focusTab && isActive) {
                    tab.focus();
                }
            });

            appTourPanels.forEach(panel => {
                const isActive = panel.dataset.appScreenPanel === screen;
                panel.classList.toggle('is-active', isActive);
                panel.hidden = !isActive;
            });

            appTourImages.forEach(image => {
                const isActive = image.dataset.appScreenImage === screen;
                image.classList.toggle('is-active', isActive);
                image.hidden = !isActive;
            });
        }

        appTourTargets.forEach(control => {
            control.addEventListener('click', () => {
                setAppTourScreen(control.dataset.appScreenTarget);
            });
        });

        appTourTabs.forEach((tab, index) => {
            tab.addEventListener('keydown', event => {
                let nextIndex = index;

                if (event.key === 'ArrowRight') {
                    nextIndex = (index + 1) % appTourTabs.length;
                } else if (event.key === 'ArrowLeft') {
                    nextIndex = (index - 1 + appTourTabs.length) % appTourTabs.length;
                } else if (event.key === 'Home') {
                    nextIndex = 0;
                } else if (event.key === 'End') {
                    nextIndex = appTourTabs.length - 1;
                } else {
                    return;
                }

                event.preventDefault();
                setAppTourScreen(appTourTabs[nextIndex].dataset.appScreenTarget, { focusTab: true });
            });
        });

        setAppTourScreen('detect');
    }


    /* -----------------------------------------------------------------
       7. Scroll Reveal
       ----------------------------------------------------------------- */
    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const delay = entry.target.dataset.revealDelay || 0;
                setTimeout(() => {
                    entry.target.classList.add('revealed');
                }, Number(delay));
                revealObserver.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.12,
        rootMargin: '0px 0px -60px 0px'
    });

    document.querySelectorAll('.bento-card').forEach((card, index) => {
        card.dataset.revealDelay = index * 100;
    });

    document.querySelectorAll('.hiw-step').forEach((step, index) => {
        step.dataset.revealDelay = index * 150;
    });

    document.querySelectorAll('[data-reveal], [data-reveal-right]').forEach(element => {
        revealObserver.observe(element);
    });


    /* -----------------------------------------------------------------
       8. Smooth Scroll
       ----------------------------------------------------------------- */
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', event => {
            const href = anchor.getAttribute('href');
            if (href === '#') {
                return;
            }

            const target = document.querySelector(href);
            if (!target) {
                return;
            }

            event.preventDefault();
            const offset = 80;
            const y = target.getBoundingClientRect().top + window.scrollY - offset;
            window.scrollTo({ top: y, behavior: 'smooth' });
        });
    });


    /* -----------------------------------------------------------------
       9. Active Nav Link
       ----------------------------------------------------------------- */
    const sections = document.querySelectorAll('section[id]');
    const navLinks = document.querySelectorAll('.nav-link');

    const sectionObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) {
                return;
            }

            const id = entry.target.id;
            navLinks.forEach(link => {
                link.style.color = link.getAttribute('href') === `#${id}`
                    ? 'var(--accent)'
                    : '';
            });
        });
    }, { threshold: 0.4 });

    sections.forEach(section => sectionObserver.observe(section));
});
