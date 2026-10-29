(function () {
    const catalog = document.querySelector("#catalogPanel");
    const scrim = document.querySelector("#scrim");
    const catalogToggles = document.querySelectorAll("[data-catalog-toggle]");
    const accountToggle = document.querySelector("#accountToggle");
    const accountMenu = document.querySelector("#accountMenu");

    function setCatalog(open) {
        if (!catalog) {
            return;
        }
        catalog.classList.toggle("is-open", open);
        if (scrim) {
            scrim.hidden = !open;
        }
        document.body.classList.toggle("nav-open", open);
        catalogToggles.forEach(function (button) {
            button.setAttribute("aria-expanded", String(open));
        });
    }

    function setAccount(open) {
        if (!accountMenu || !accountToggle) {
            return;
        }
        accountMenu.classList.toggle("is-open", open);
        accountToggle.setAttribute("aria-expanded", String(open));
    }

    catalogToggles.forEach(function (button) {
        button.addEventListener("click", function () {
            setAccount(false);
            setCatalog(!catalog.classList.contains("is-open"));
        });
    });

    if (accountToggle) {
        accountToggle.addEventListener("click", function (event) {
            event.stopPropagation();
            setCatalog(false);
            setAccount(!accountMenu.classList.contains("is-open"));
        });
    }

    if (scrim) {
        scrim.addEventListener("click", function () {
            setCatalog(false);
            setAccount(false);
        });
    }

    document.addEventListener("click", function (event) {
        if (accountMenu && accountToggle && !accountMenu.contains(event.target) && !accountToggle.contains(event.target)) {
            setAccount(false);
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            setCatalog(false);
            setAccount(false);
        }
    });

    const header = document.querySelector(".site-header");
    if (header) {
        const onScroll = function () {
            header.classList.toggle("is-stuck", window.scrollY > 4);
        };
        onScroll();
        window.addEventListener("scroll", onScroll, { passive: true });
    }

    const hero = document.querySelector("[data-hero]");
    if (hero) {
        const slides = Array.prototype.slice.call(hero.querySelectorAll(".hero-slide"));
        const dots = Array.prototype.slice.call(hero.querySelectorAll("[data-hero-dot]"));
        let index = 0;
        let timer = null;
        const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

        function show(next) {
            if (!slides.length) {
                return;
            }
            index = (next + slides.length) % slides.length;
            slides.forEach(function (slide, i) {
                slide.classList.toggle("is-active", i === index);
            });
            dots.forEach(function (dot, i) {
                dot.classList.toggle("is-active", i === index);
            });
        }

        function restart() {
            window.clearInterval(timer);
            if (!reduced && slides.length > 1) {
                timer = window.setInterval(function () {
                    show(index + 1);
                }, 5600);
            }
        }

        const prev = hero.querySelector("[data-hero-prev]");
        const next = hero.querySelector("[data-hero-next]");
        if (prev) {
            prev.addEventListener("click", function () {
                show(index - 1);
                restart();
            });
        }
        if (next) {
            next.addEventListener("click", function () {
                show(index + 1);
                restart();
            });
        }
        dots.forEach(function (dot) {
            dot.addEventListener("click", function () {
                show(Number(dot.getAttribute("data-hero-dot")));
                restart();
            });
        });
        hero.addEventListener("mouseenter", function () {
            window.clearInterval(timer);
        });
        hero.addEventListener("mouseleave", restart);
        restart();
    }

    document.querySelectorAll("form.buy").forEach(function (form) {
        form.addEventListener("submit", function () {
            const button = form.querySelector("button[type='submit']");
            if (!button) {
                return;
            }
            button.classList.add("is-loading");
            button.textContent = "Добавляем…";
        });
    });

    document.querySelectorAll("[data-stepper]").forEach(function (box) {
        const input = box.querySelector("input");
        const form = box.closest("form");
        if (!input) {
            return;
        }
        box.querySelectorAll("[data-step]").forEach(function (button) {
            button.addEventListener("click", function () {
                const min = input.min === "" ? -Infinity : Number(input.min);
                const max = input.max === "" ? Infinity : Number(input.max);
                let value = Number(input.value || 0) + Number(button.getAttribute("data-step"));
                if (value < min) {
                    value = min;
                }
                if (value > max) {
                    value = max;
                }
                input.value = String(value);
                if (box.getAttribute("data-autosubmit") === "true" && form) {
                    if (typeof form.requestSubmit === "function") {
                        form.requestSubmit();
                    } else {
                        form.submit();
                    }
                }
            });
        });
    });

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    document.querySelectorAll("form.pay-sim").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (form.dataset.ready === "true" || reduced) {
                return;
            }
            event.preventDefault();
            const button = form.querySelector("button[type='submit']");
            if (button) {
                button.disabled = true;
                button.textContent = "Проводим оплату…";
            }
            window.setTimeout(function () {
                form.dataset.ready = "true";
                if (button) {
                    button.disabled = false;
                }
                if (typeof form.requestSubmit === "function") {
                    form.requestSubmit();
                } else {
                    form.submit();
                }
            }, 900);
        });
    });
})();
