(function () {
    "use strict";

    const $ = (selector, root) => (root || document).querySelector(selector);
    const $$ = (selector, root) => Array.from((root || document).querySelectorAll(selector));
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const session = ($("[data-session]") || { dataset: { session: "staff" } }).dataset.session;

    const store = {
        get(key, fallback) {
            try {
                const raw = window.localStorage.getItem(key);
                return raw == null ? fallback : JSON.parse(raw);
            } catch (e) {
                return fallback;
            }
        },
        set(key, value) {
            try {
                window.localStorage.setItem(key, JSON.stringify(value));
            } catch (e) {
                return;
            }
        }
    };

    function rub(value) {
        const amount = Number(value);
        if (!Number.isFinite(amount)) {
            return "";
        }
        return new Intl.NumberFormat("ru-RU", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount) + " ₽";
    }

    function monthly(value) {
        const amount = Math.round(Number(value) / 12);
        return new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 }).format(amount) + " ₽";
    }

    function el(tag, attrs, children) {
        const node = document.createElement(tag);
        Object.entries(attrs || {}).forEach(([key, value]) => {
            if (value == null || value === false) {
                return;
            }
            if (key === "text") {
                node.textContent = value;
            } else if (key === "class") {
                node.className = value;
            } else {
                node.setAttribute(key, value === true ? "" : value);
            }
        });
        (children || []).forEach((child) => child && node.appendChild(child));
        return node;
    }

    function thumb(photo, emoji, alt) {
        const box = el("span", { class: "thumb" });
        if (photo) {
            const img = el("img", { src: photo, alt: alt || "", loading: "lazy" });
            img.addEventListener("error", () => {
                img.remove();
                box.textContent = emoji || "🛍️";
            });
            box.appendChild(img);
        } else {
            box.textContent = emoji || "🛍️";
        }
        return box;
    }

    function productFrom(node) {
        const host = node && node.closest("[data-product]");
        if (!host) {
            return null;
        }
        const d = host.dataset;
        return {
            id: Number(d.product),
            name: d.name,
            sku: d.sku,
            price: d.price,
            month: d.month,
            desc: d.desc || "",
            stock: Number(d.stock || 0),
            active: d.active !== "false",
            cat: d.cat,
            photo: d.photo || "",
            emoji: d.emoji || "🛍️",
            url: d.url
        };
    }

    /* ---------- Theme ---------- */
    const root = document.documentElement;
    function applyTheme(theme) {
        root.setAttribute("data-theme", theme);
        try {
            window.localStorage.setItem("nova-theme", theme);
        } catch (e) {
            return;
        }
    }
    document.addEventListener("click", (event) => {
        const toggle = event.target.closest("[data-theme-toggle]");
        if (!toggle) {
            return;
        }
        event.preventDefault();
        const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
        if (document.startViewTransition && !reducedMotion) {
            document.startViewTransition(() => applyTheme(next));
        } else {
            applyTheme(next);
        }
    });

    /* ---------- Toasts ---------- */
    const toastHost = $("[data-toasts]");
    function toast(options) {
        if (!toastHost) {
            return;
        }
        const life = options.life || 4200;
        const item = el("div", { class: "toast " + (options.kind || "ok"), role: options.kind === "err" ? "alert" : "status" });
        item.style.setProperty("--life", life + "ms");
        let media;
        if (options.product) {
            media = thumb(options.product.photo, options.product.emoji, "");
        } else {
            media = el("span", { class: "thumb icon-only", text: options.icon || (options.kind === "err" ? "!" : "✓") });
        }
        const body = el("div", {}, [el("b", { text: options.title }), el("small", { text: options.text || "" })]);
        const action = options.href ? el("a", { href: options.href, text: options.action || "Открыть" }) : el("span");
        const close = el("button", { type: "button", class: "close", "aria-label": "Закрыть", text: "×" });
        item.append(media, body, action, close, el("span", { class: "life" }));
        toastHost.appendChild(item);
        while (toastHost.children.length > 3) {
            toastHost.firstElementChild.remove();
        }
        let timer;
        const dismiss = () => {
            window.clearTimeout(timer);
            item.classList.add("is-out");
            item.addEventListener("animationend", () => item.remove(), { once: true });
        };
        const arm = (ms) => {
            timer = window.setTimeout(dismiss, ms);
        };
        close.addEventListener("click", dismiss);
        item.addEventListener("mouseenter", () => window.clearTimeout(timer));
        item.addEventListener("mouseleave", () => arm(1600));
        arm(life);
    }

    /* ---------- Header, panels, menus ---------- */
    const header = $(".site-header");
    const catalog = $("#catalogPanel");
    const scrim = $("#scrim");
    const accountToggle = $("#accountToggle");
    const accountMenu = $("#accountMenu");

    function setCatalog(open) {
        if (!catalog) {
            return;
        }
        catalog.classList.toggle("is-open", open);
        if (scrim) {
            scrim.hidden = !open;
        }
        $$("[data-catalog-toggle]").forEach((button) => button.setAttribute("aria-expanded", String(open)));
        if (open && header) {
            header.classList.remove("is-hidden");
            if (window.scrollY > 200 && window.innerWidth < 720) {
                window.scrollTo({ top: 0, behavior: reducedMotion ? "auto" : "smooth" });
            }
        }
    }

    function setAccount(open) {
        if (!accountMenu || !accountToggle) {
            return;
        }
        accountMenu.classList.toggle("is-open", open);
        accountToggle.setAttribute("aria-expanded", String(open));
    }

    $$("[data-catalog-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            setAccount(false);
            setCatalog(!catalog.classList.contains("is-open"));
        });
    });
    if (accountToggle) {
        accountToggle.addEventListener("click", (event) => {
            event.stopPropagation();
            setCatalog(false);
            setAccount(!accountMenu.classList.contains("is-open"));
        });
    }
    if (scrim) {
        scrim.addEventListener("click", () => {
            setCatalog(false);
            setAccount(false);
        });
    }
    document.addEventListener("click", (event) => {
        if (accountMenu && !accountMenu.contains(event.target) && !accountToggle.contains(event.target)) {
            setAccount(false);
        }
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            setCatalog(false);
            setAccount(false);
            closeSuggest();
        }
        const typing = /^(INPUT|TEXTAREA|SELECT)$/.test((event.target.tagName || "")) || event.target.isContentEditable;
        if (event.key === "/" && !typing) {
            const input = $("[data-search-input]");
            if (input) {
                event.preventDefault();
                input.focus();
                input.select();
            }
        }
    });

    const toTop = $("[data-to-top]");
    let lastY = window.scrollY;
    let ticking = false;
    function onScroll() {
        const y = window.scrollY;
        if (header) {
            header.classList.toggle("is-stuck", y > 4);
            const panelOpen = catalog && catalog.classList.contains("is-open");
            const hide = y > 260 && y > lastY + 2 && !panelOpen && !document.activeElement.closest(".site-header");
            if (hide) {
                header.classList.add("is-hidden");
            } else if (y < lastY - 2 || y < 260) {
                header.classList.remove("is-hidden");
            }
            document.body.classList.toggle("header-hidden", header.classList.contains("is-hidden"));
        }
        if (toTop) {
            const max = document.documentElement.scrollHeight - window.innerHeight;
            toTop.classList.toggle("is-on", y > 700);
            toTop.style.setProperty("--sp", max > 0 ? (y / max).toFixed(3) : 0);
        }
        lastY = y;
        ticking = false;
    }
    window.addEventListener("scroll", () => {
        if (!ticking) {
            ticking = true;
            window.requestAnimationFrame(onScroll);
        }
    }, { passive: true });
    onScroll();
    if (toTop) {
        toTop.addEventListener("click", () => window.scrollTo({ top: 0, behavior: reducedMotion ? "auto" : "smooth" }));
    }

    $$("[data-tab]").forEach((tab) => {
        const path = tab.getAttribute("data-tab");
        const here = window.location.pathname;
        if (path === "/" ? here === "/" : here.startsWith(path)) {
            tab.classList.add("is-active");
        }
    });

    document.addEventListener("pointermove", (event) => {
        const button = event.target.closest && event.target.closest(".btn");
        if (!button) {
            return;
        }
        const rect = button.getBoundingClientRect();
        button.style.setProperty("--mx", (event.clientX - rect.left) + "px");
        button.style.setProperty("--my", (event.clientY - rect.top) + "px");
    }, { passive: true });

    /* ---------- Reveal & lazy images ---------- */
    const revealTargets = $$("[data-reveal]");
    if ("IntersectionObserver" in window && !reducedMotion) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-in");
                    observer.unobserve(entry.target);
                }
            });
        }, { rootMargin: "0px 0px -40px 0px", threshold: 0.08 });
        revealTargets.forEach((node) => observer.observe(node));
        window.setTimeout(() => revealTargets.forEach((node) => {
            if (node.getBoundingClientRect().top < window.innerHeight) {
                node.classList.add("is-in");
            }
        }), 60);
    } else {
        revealTargets.forEach((node) => node.classList.add("is-in"));
    }
    window.addEventListener("beforeprint", () => revealTargets.forEach((node) => node.classList.add("is-in")));

    $$("img[data-lazy]").forEach((img) => {
        const box = img.parentElement;
        const done = () => {
            img.classList.add("is-loaded");
            box.classList.remove("is-loading");
        };
        if (img.complete && img.naturalWidth > 0) {
            done();
        } else {
            box.classList.add("is-loading");
            img.addEventListener("load", done, { once: true });
            img.addEventListener("error", done, { once: true });
        }
    });

    /* ---------- Hero slider ---------- */
    const hero = $("[data-hero]");
    if (hero) {
        const slides = $$(".hero-slide", hero);
        const dots = $$("[data-hero-dot]", hero);
        const duration = 6500;
        let index = 0;
        let timer = null;
        let paused = false;

        const show = (next) => {
            index = (next + slides.length) % slides.length;
            slides.forEach((slide, i) => {
                slide.classList.toggle("is-active", i === index);
                slide.setAttribute("aria-hidden", String(i !== index));
                if (i === index) {
                    $$("img[loading='lazy']", slide).forEach((img) => img.setAttribute("loading", "eager"));
                }
            });
            dots.forEach((dot, i) => {
                dot.classList.remove("is-active");
                if (i === index) {
                    void dot.offsetWidth;
                    dot.classList.add("is-active");
                }
            });
            schedule();
        };
        const schedule = () => {
            window.clearTimeout(timer);
            if (!paused && !reducedMotion && slides.length > 1) {
                timer = window.setTimeout(() => show(index + 1), duration);
            }
        };
        const pause = (value) => {
            paused = value;
            hero.classList.toggle("is-paused", value);
            if (value) {
                window.clearTimeout(timer);
            } else {
                schedule();
            }
        };
        hero.style.setProperty("--dur", duration + "ms");
        dots.forEach((dot) => dot.style.setProperty("--dur", duration + "ms"));
        if (reducedMotion) {
            hero.classList.add("is-paused");
        }
        const prev = $("[data-hero-prev]", hero);
        const next = $("[data-hero-next]", hero);
        prev && prev.addEventListener("click", () => show(index - 1));
        next && next.addEventListener("click", () => show(index + 1));
        dots.forEach((dot) => dot.addEventListener("click", () => show(Number(dot.dataset.heroDot))));
        hero.addEventListener("mouseenter", () => pause(true));
        hero.addEventListener("mouseleave", () => pause(false));
        hero.addEventListener("focusin", () => pause(true));
        hero.addEventListener("focusout", () => pause(false));
        document.addEventListener("visibilitychange", () => pause(document.hidden));
        hero.addEventListener("keydown", (event) => {
            if (event.key === "ArrowLeft") {
                show(index - 1);
            } else if (event.key === "ArrowRight") {
                show(index + 1);
            }
        });

        let startX = null;
        hero.addEventListener("pointerdown", (event) => {
            startX = event.clientX;
        });
        hero.addEventListener("pointerup", (event) => {
            if (startX == null) {
                return;
            }
            const dx = event.clientX - startX;
            if (Math.abs(dx) > 50) {
                show(index + (dx < 0 ? 1 : -1));
            }
            startX = null;
        });

        if (!reducedMotion && window.matchMedia("(pointer: fine)").matches) {
            hero.addEventListener("pointermove", (event) => {
                const rect = hero.getBoundingClientRect();
                const px = ((event.clientX - rect.left) / rect.width - 0.5) * 30;
                const py = ((event.clientY - rect.top) / rect.height - 0.5) * 30;
                $$("[data-parallax]", hero).forEach((stage) => {
                    stage.style.setProperty("--px", px.toFixed(1));
                    stage.style.setProperty("--py", py.toFixed(1));
                });
            });
        }
        schedule();
    }

    /* ---------- Live search ---------- */
    const searchForm = $("[data-search]");
    const searchInput = $("[data-search-input]");
    const suggest = $("[data-suggest]");
    let suggestItems = [];
    let suggestIndex = -1;
    let searchController = null;
    let searchTimer = null;

    function closeSuggest() {
        if (!suggest) {
            return;
        }
        suggest.classList.remove("is-open");
        searchInput && searchInput.setAttribute("aria-expanded", "false");
        suggestIndex = -1;
    }

    function highlight(text, query) {
        const fragment = document.createDocumentFragment();
        const lower = text.toLowerCase();
        const q = query.toLowerCase();
        let from = 0;
        let at = lower.indexOf(q);
        if (!q || at < 0) {
            fragment.appendChild(document.createTextNode(text));
            return fragment;
        }
        while (at >= 0) {
            fragment.appendChild(document.createTextNode(text.slice(from, at)));
            fragment.appendChild(el("mark", { text: text.slice(at, at + q.length) }));
            from = at + q.length;
            at = lower.indexOf(q, from);
        }
        fragment.appendChild(document.createTextNode(text.slice(from)));
        return fragment;
    }

    function renderSuggest(products, query) {
        suggest.textContent = "";
        suggestItems = [];
        suggestIndex = -1;
        if (!products.length) {
            suggest.appendChild(el("div", { class: "suggest-empty", text: "По запросу «" + query + "» ничего нет" }));
        } else {
            products.slice(0, 6).forEach((product, i) => {
                const name = el("strong");
                name.appendChild(highlight(product.name, query));
                const link = el("a", { class: "suggest-item", href: "/products/" + product.id, role: "option", id: "sg-" + i }, [
                    thumb("/img/products/" + encodeURIComponent(product.sku) + ".jpg", "🛍️", ""),
                    el("span", {}, [name, el("small", { text: product.categoryName + " · " + (product.stock > 0 ? "в наличии" : "нет в наличии") })]),
                    el("span", { class: "price", text: rub(product.price) })
                ]);
                suggest.appendChild(link);
                suggestItems.push(link);
            });
            suggest.appendChild(el("a", {
                class: "suggest-foot",
                href: "/?q=" + encodeURIComponent(query) + "#catalog",
                text: products.length > 6 ? "Все результаты (" + products.length + ") →" : "Показать в каталоге →"
            }));
        }
        suggest.classList.add("is-open");
        searchInput.setAttribute("aria-expanded", "true");
    }

    function moveSuggest(step) {
        if (!suggestItems.length) {
            return;
        }
        suggestIndex = (suggestIndex + step + suggestItems.length) % suggestItems.length;
        suggestItems.forEach((item, i) => item.classList.toggle("is-active", i === suggestIndex));
        searchInput.setAttribute("aria-activedescendant", suggestItems[suggestIndex].id);
        suggestItems[suggestIndex].scrollIntoView({ block: "nearest" });
    }

    if (searchForm && searchInput && suggest && typeof window.fetch === "function") {
        searchInput.addEventListener("input", () => {
            const query = searchInput.value.trim();
            window.clearTimeout(searchTimer);
            if (query.length < 2) {
                closeSuggest();
                return;
            }
            searchTimer = window.setTimeout(() => {
                if (searchController) {
                    searchController.abort();
                }
                searchController = new AbortController();
                window.fetch("/api/products?q=" + encodeURIComponent(query), {
                    headers: { Accept: "application/json" },
                    signal: searchController.signal
                })
                    .then((response) => (response.ok ? response.json() : []))
                    .then((products) => {
                        if (searchInput.value.trim() === query) {
                            renderSuggest(products, query);
                        }
                    })
                    .catch(() => {});
            }, 180);
        });
        searchInput.addEventListener("keydown", (event) => {
            if (!suggest.classList.contains("is-open")) {
                return;
            }
            if (event.key === "ArrowDown") {
                event.preventDefault();
                moveSuggest(1);
            } else if (event.key === "ArrowUp") {
                event.preventDefault();
                moveSuggest(-1);
            } else if (event.key === "Enter" && suggestIndex >= 0) {
                event.preventDefault();
                window.location.href = suggestItems[suggestIndex].href;
            }
        });
        searchInput.addEventListener("focus", () => {
            if (suggest.childElementCount && searchInput.value.trim().length >= 2) {
                suggest.classList.add("is-open");
            }
        });
        document.addEventListener("click", (event) => {
            if (!searchForm.contains(event.target)) {
                closeSuggest();
            }
        });
    }

    /* ---------- Favorites ---------- */
    const FAV_KEY = "nova-favs";
    const favDrawer = $("[data-fav-drawer]");
    const favList = $("[data-fav-list]");
    const favEmpty = $("[data-fav-empty]");
    const favs = () => store.get(FAV_KEY, []);

    function syncFavs() {
        const list = favs();
        const ids = new Set(list.map((item) => item.id));
        $$("[data-fav]").forEach((button) => {
            const product = productFrom(button);
            button.setAttribute("aria-pressed", String(!!product && ids.has(product.id)));
        });
        $$("[data-fav-count]").forEach((badge) => {
            badge.hidden = list.length === 0;
            badge.textContent = String(list.length);
        });
        if (favDrawer && favDrawer.open) {
            renderFavs();
        }
    }

    function renderFavs() {
        if (!favList) {
            return;
        }
        const list = favs();
        favList.textContent = "";
        if (!list.length && favEmpty) {
            favList.appendChild(favEmpty.content.cloneNode(true));
            return;
        }
        list.forEach((item, i) => {
            const remove = el("button", { type: "button", "aria-label": "Убрать из избранного", text: "✕" });
            remove.addEventListener("click", () => {
                store.set(FAV_KEY, favs().filter((fav) => fav.id !== item.id));
                syncFavs();
            });
            const row = el("div", { class: "fav-item" }, [
                thumb(item.photo, item.emoji, item.name),
                el("a", { href: item.url }, [el("strong", { text: item.name }), el("small", { text: item.price })]),
                remove
            ]);
            row.style.setProperty("--i", i);
            favList.appendChild(row);
        });
    }

    document.addEventListener("click", (event) => {
        const favButton = event.target.closest("[data-fav]");
        if (favButton) {
            event.preventDefault();
            const product = productFrom(favButton);
            if (!product) {
                return;
            }
            let list = favs();
            const exists = list.some((item) => item.id === product.id);
            if (exists) {
                list = list.filter((item) => item.id !== product.id);
            } else {
                list.unshift({ id: product.id, name: product.name, price: product.price, photo: product.photo, emoji: product.emoji, url: product.url });
            }
            store.set(FAV_KEY, list.slice(0, 40));
            favButton.classList.remove("is-burst");
            void favButton.offsetWidth;
            favButton.classList.add("is-burst");
            syncFavs();
            toast({
                kind: "ok",
                product: product,
                title: exists ? "Убрано из избранного" : "В избранном",
                text: product.name,
                life: 2600
            });
            return;
        }
        const opener = event.target.closest("[data-fav-open]");
        if (opener && favDrawer && typeof favDrawer.showModal === "function") {
            event.preventDefault();
            setCatalog(false);
            renderFavs();
            favDrawer.showModal();
        }
    });
    window.addEventListener("storage", (event) => {
        if (event.key === FAV_KEY) {
            syncFavs();
        }
    });
    syncFavs();

    $$("dialog").forEach((dialog) => {
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog || event.target.closest("[data-dialog-close]")) {
                dialog.close();
            }
        });
        dialog.addEventListener("close", () => document.body.classList.remove("is-locked"));
    });

    /* ---------- Quick view ---------- */
    const quick = $("[data-quick-view]");
    document.addEventListener("click", (event) => {
        const trigger = event.target.closest("[data-quick]");
        if (!trigger || !quick || typeof quick.showModal !== "function") {
            return;
        }
        event.preventDefault();
        const product = productFrom(trigger);
        if (!product) {
            return;
        }
        quick.setAttribute("data-product", product.id);
        Object.entries({
            product: product.id, name: product.name, sku: product.sku, price: product.price, month: product.month,
            desc: product.desc, stock: product.stock, active: product.active, cat: product.cat, photo: product.photo,
            emoji: product.emoji, url: product.url
        }).forEach(([key, value]) => {
            quick.dataset[key] = value;
        });
        const art = $("[data-qv-art]", quick);
        art.textContent = "";
        if (product.photo) {
            art.appendChild(el("img", { src: product.photo, alt: product.name }));
        } else {
            art.textContent = product.emoji;
        }
        $("[data-qv-cat]", quick).textContent = product.cat;
        $("[data-qv-name]", quick).textContent = product.name;
        $("[data-qv-desc]", quick).textContent = product.desc;
        $("[data-qv-price]", quick).textContent = product.price;
        $("[data-qv-month]", quick).textContent = product.month;
        $("[data-qv-stock]", quick).textContent = product.stock > 0 ? product.stock + " шт." : "нет";
        $("[data-qv-bar]", quick).style.setProperty("--w", Math.max(4, Math.min(100, product.stock * 100 / 40)) + "%");
        $("[data-qv-link]", quick).href = product.url;
        const available = product.active && product.stock > 0;
        const form = $("[data-qv-form]", quick);
        const qty = $("[data-qv-qty]", quick);
        $("[data-qv-id]", quick).value = product.id;
        form.dataset.productName = product.name;
        qty.value = "1";
        qty.max = String(product.stock);
        form.hidden = !(available && session === "customer");
        $("[data-qv-login]", quick).hidden = !(available && session === "guest");
        const submit = $("button[type='submit']", form);
        submit.textContent = "В корзину";
        submit.classList.remove("is-done");
        quick.showModal();
    });

    /* ---------- Stepper ---------- */
    document.addEventListener("click", (event) => {
        const button = event.target.closest("[data-step]");
        if (!button) {
            return;
        }
        const box = button.closest("[data-stepper]");
        const input = box && $("input", box);
        if (!input) {
            return;
        }
        const min = input.min === "" ? -Infinity : Number(input.min);
        const max = input.max === "" ? Infinity : Number(input.max);
        const current = Number(input.value || 0);
        let value = current + Number(button.dataset.step);
        value = Math.min(max, Math.max(min, value));
        if (value === current) {
            box.animate([{ transform: "translateX(0)" }, { transform: "translateX(-4px)" }, { transform: "translateX(4px)" }, { transform: "translateX(0)" }], { duration: 260 });
            if (value === max && Number(button.dataset.step) > 0) {
                toast({ kind: "err", title: "Больше нет на складе", text: "Доступно " + max + " шт.", life: 2400 });
            }
            return;
        }
        input.value = String(value);
        input.dispatchEvent(new Event("change", { bubbles: true }));
    });

    /* ---------- Add to cart ---------- */
    function setCartCount(count) {
        $$("[data-cart-link]").forEach((link) => {
            let badge = $(".badge", link);
            if (count < 1) {
                badge && badge.remove();
                return;
            }
            if (!badge) {
                badge = el("span", { class: "badge" });
                link.appendChild(badge);
            }
            badge.textContent = String(count);
            badge.classList.remove("is-pop");
            void badge.offsetWidth;
            badge.classList.add("is-pop");
        });
    }

    function cartUnits(cart) {
        return (cart.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
    }

    function visibleCartTarget() {
        return $$("[data-cart-link]").find((link) => link.offsetParent !== null && link.getBoundingClientRect().width > 0);
    }

    function flyToCart(source) {
        const target = visibleCartTarget();
        if (!source || !target || reducedMotion || typeof source.animate !== "function") {
            return Promise.resolve();
        }
        if (header) {
            header.classList.remove("is-hidden");
        }
        const from = source.getBoundingClientRect();
        const to = target.getBoundingClientRect();
        const size = Math.min(from.width, 160);
        const flyer = el("div", { class: "flyer" });
        flyer.style.cssText = "width:" + size + "px;height:" + size + "px;left:" + (from.left + (from.width - size) / 2) + "px;top:" + (from.top + (from.height - size) / 2) + "px";
        if (source.tagName === "IMG") {
            flyer.appendChild(el("img", { src: source.currentSrc || source.src, alt: "" }));
        }
        document.body.appendChild(flyer);
        const dx = to.left + to.width / 2 - (from.left + from.width / 2);
        const dy = to.top + to.height / 2 - (from.top + from.height / 2);
        const animation = flyer.animate([
            { transform: "translate(0,0) scale(1)", opacity: 1, borderRadius: "18px" },
            { transform: "translate(" + dx * 0.35 + "px," + (dy * 0.35 - 120) + "px) scale(.6)", opacity: 1, offset: 0.45 },
            { transform: "translate(" + dx + "px," + dy + "px) scale(.12)", opacity: 0.4, borderRadius: "50%" }
        ], { duration: 820, easing: "cubic-bezier(.5,0,.3,1)" });
        return animation.finished.then(() => {
            flyer.remove();
            target.classList.remove("cart-shake");
            void target.offsetWidth;
            target.classList.add("cart-shake");
        }).catch(() => flyer.remove());
    }

    document.addEventListener("submit", (event) => {
        const form = event.target.closest("form.buy");
        if (!form || typeof window.fetch !== "function") {
            return;
        }
        event.preventDefault();
        const button = $("button[type='submit']", form);
        const productId = Number(($("[name='productId']", form) || {}).value);
        const quantity = Number(($("[name='quantity']", form) || {}).value);
        const product = productFrom(form) || { name: form.dataset.productName || "Товар", photo: "", emoji: "🛍️" };
        if (!productId || !(quantity >= 1)) {
            toast({ kind: "err", title: "Не добавлено", text: "Укажите количество больше нуля" });
            return;
        }
        if (button) {
            button.disabled = true;
            button.classList.add("is-loading");
        }
        const scope = form.closest("[data-product], dialog, .product") || document;
        const source = $(".art img, .zoom img, [data-qv-art] img", scope);
        window.fetch("/api/cart/items", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify({ productId: productId, quantity: quantity })
        }).then((response) => {
            const type = response.headers.get("content-type") || "";
            if (!type.includes("json")) {
                throw new Error("Сессия истекла. Войдите снова и повторите.");
            }
            return response.json().then((body) => {
                if (!response.ok) {
                    throw new Error(body.message || "Не удалось добавить товар");
                }
                return body;
            });
        }).then((cart) => {
            const line = (cart.items || []).find((item) => Number(item.productId) === productId);
            const inCart = line ? line.quantity : quantity;
            if (button) {
                button.classList.remove("is-loading");
                button.classList.add("is-done");
            }
            const closeQuick = form.closest("dialog");
            const flight = flyToCart(source);
            if (closeQuick) {
                window.setTimeout(() => closeQuick.close(), 250);
            }
            return flight.then(() => {
                setCartCount(cartUnits(cart));
                toast({
                    kind: "ok",
                    product: product,
                    title: "Добавлено в корзину",
                    text: product.name + " · " + inCart + " шт. · итого " + rub(cart.total),
                    href: "/cart",
                    action: "В корзину →"
                });
            });
        }).catch((error) => {
            toast({ kind: "err", title: "Не добавлено", text: error.message || "Не удалось добавить товар" });
        }).finally(() => {
            if (!button) {
                return;
            }
            button.disabled = false;
            button.classList.remove("is-loading");
            window.setTimeout(() => button.classList.remove("is-done"), 1400);
        });
    });

    /* ---------- Cart page ---------- */
    const cartBox = $("[data-cart]");
    function animateNumber(node, to, format) {
        if (!node) {
            return;
        }
        const from = Number(node.dataset.value || to);
        node.dataset.value = to;
        if (reducedMotion || from === to) {
            node.textContent = format(to);
            return;
        }
        const start = performance.now();
        const step = (now) => {
            const t = Math.min(1, (now - start) / 500);
            const eased = 1 - Math.pow(1 - t, 3);
            node.textContent = format(from + (to - from) * eased);
            if (t < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    }

    function applyCart(cart) {
        const items = cart.items || [];
        const units = cartUnits(cart);
        const ids = new Set(items.map((item) => Number(item.productId)));
        $$("[data-cart-row]", cartBox).forEach((row) => {
            const id = Number(row.dataset.cartRow);
            if (!ids.has(id)) {
                row.classList.add("is-removing");
                window.setTimeout(() => row.remove(), 380);
            }
        });
        items.forEach((item) => {
            const row = $("[data-cart-row='" + item.productId + "']", cartBox);
            if (!row) {
                return;
            }
            const input = $("input[name='quantity']", row);
            if (input && document.activeElement !== input) {
                input.value = item.quantity;
            }
            animateNumber($("[data-line-total]", row), Number(item.lineTotal), rub);
        });
        animateNumber($("[data-cart-sum]"), Number(cart.total), rub);
        animateNumber($("[data-cart-total]"), Number(cart.total), rub);
        animateNumber($("[data-cart-units]"), units, (v) => String(Math.round(v)));
        const month = $("[data-cart-month]");
        if (month) {
            month.textContent = "или от " + monthly(cart.total) + " / мес";
        }
        const positions = $("[data-cart-positions]");
        if (positions) {
            positions.textContent = "Позиций: " + items.length;
        }
        setCartCount(units);
        if (!items.length) {
            window.setTimeout(() => {
                cartBox.remove();
                const flow = $(".flow");
                flow && flow.remove();
                positions && positions.remove();
                const empty = $("[data-cart-empty]");
                if (empty) {
                    empty.hidden = false;
                    empty.animate([{ opacity: 0, transform: "translateY(16px)" }, { opacity: 1, transform: "none" }], { duration: 450, easing: "ease-out" });
                }
            }, 400);
        }
    }

    function cartRequest(method, productId, body) {
        return window.fetch("/api/cart/items/" + productId, {
            method: method,
            credentials: "same-origin",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: body ? JSON.stringify(body) : undefined
        }).then((response) => {
            const type = response.headers.get("content-type") || "";
            if (!type.includes("json")) {
                throw new Error("Сессия истекла. Обновите страницу.");
            }
            return response.json().then((data) => {
                if (!response.ok) {
                    throw new Error(data.message || "Не удалось обновить корзину");
                }
                return data;
            });
        });
    }

    if (cartBox && typeof window.fetch === "function") {
        $$("[data-cart-row]", cartBox).forEach((row) => {
            const id = row.dataset.cartRow;
            const qtyForm = $("[data-qty-form]", row);
            const input = qtyForm && $("input[name='quantity']", qtyForm);
            const total = $("[data-line-total]", row);
            if (total) {
                total.dataset.value = Number(row.dataset.unit) * Number(input ? input.value : 1);
            }
            let timer = null;
            let lastGood = input ? input.value : "1";
            const push = () => {
                const quantity = Math.max(0, Math.floor(Number(input.value || 0)));
                row.style.opacity = ".6";
                cartRequest("PATCH", id, { quantity: quantity })
                    .then((cart) => {
                        lastGood = String(quantity);
                        applyCart(cart);
                        if (quantity === 0) {
                            toast({ kind: "ok", title: "Товар убран", text: "Корзина обновлена", life: 2200 });
                        }
                    })
                    .catch((error) => {
                        input.value = lastGood;
                        toast({ kind: "err", title: "Не получилось", text: error.message });
                    })
                    .finally(() => {
                        row.style.opacity = "";
                    });
            };
            if (qtyForm && input) {
                qtyForm.addEventListener("submit", (event) => {
                    event.preventDefault();
                    window.clearTimeout(timer);
                    push();
                });
                input.addEventListener("change", () => {
                    window.clearTimeout(timer);
                    timer = window.setTimeout(push, 320);
                });
            }
            const removeForm = $("[data-remove-form]", row);
            if (removeForm) {
                removeForm.addEventListener("submit", (event) => {
                    event.preventDefault();
                    row.classList.add("is-removing");
                    cartRequest("DELETE", id)
                        .then((cart) => {
                            applyCart(cart);
                            toast({ kind: "ok", title: "Товар убран из корзины", text: $("h2", row).textContent, life: 2600 });
                        })
                        .catch((error) => {
                            row.classList.remove("is-removing");
                            toast({ kind: "err", title: "Не получилось", text: error.message });
                        });
                });
            }
        });
        $$("[data-cart-sum], [data-cart-total]").forEach((node) => {
            node.dataset.value = $$("[data-cart-row]", cartBox).reduce((acc, row) => {
                const input = $("input[name='quantity']", row);
                return acc + Number(row.dataset.unit) * Number(input ? input.value : 0);
            }, 0);
        });
        const units = $("[data-cart-units]");
        if (units) {
            units.dataset.value = Number(units.textContent) || 0;
        }
    } else {
        $$("[data-stepper][data-autosubmit='true'] input").forEach((input) => {
            input.addEventListener("change", () => {
                const form = input.closest("form");
                form && (form.requestSubmit ? form.requestSubmit() : form.submit());
            });
        });
    }

    /* ---------- Sorting & view ---------- */
    $$("form[data-autosubmit] select").forEach((select) => {
        select.addEventListener("change", () => {
            const form = select.form;
            const url = new URL(form.action, window.location.origin);
            new FormData(form).forEach((value, key) => {
                if (value !== "") {
                    url.searchParams.set(key, value);
                }
            });
            url.hash = "catalog";
            window.location.href = url.toString();
        });
    });

    const grid = $("[data-grid]");
    const viewButtons = $$("[data-view]");
    function setView(view) {
        if (!grid) {
            return;
        }
        grid.classList.toggle("is-list", view === "list");
        viewButtons.forEach((button) => button.classList.toggle("is-active", button.dataset.view === view));
        store.set("nova-view", view);
    }
    viewButtons.forEach((button) => button.addEventListener("click", () => {
        const view = button.dataset.view;
        if (document.startViewTransition && !reducedMotion) {
            document.startViewTransition(() => setView(view));
        } else {
            setView(view);
        }
    }));
    if (grid) {
        setView(store.get("nova-view", "grid"));
    }

    /* ---------- Recently viewed ---------- */
    const RECENT_KEY = "nova-recent";
    const productPage = $("[data-product-page]");
    if (productPage) {
        const product = productFrom(productPage);
        if (product) {
            const list = store.get(RECENT_KEY, []).filter((item) => item.id !== product.id);
            list.unshift({ id: product.id, name: product.name, price: product.price, photo: product.photo, emoji: product.emoji, url: product.url });
            store.set(RECENT_KEY, list.slice(0, 10));
        }
    }
    const recent = $("[data-recent]");
    if (recent) {
        const currentId = productPage ? Number(productPage.dataset.product) : null;
        const list = store.get(RECENT_KEY, []).filter((item) => item.id !== currentId);
        const box = $("[data-recent-list]", recent);
        if (list.length && box) {
            list.forEach((item) => {
                box.appendChild(el("a", { href: item.url }, [
                    thumb(item.photo, item.emoji, ""),
                    el("span", {}, [el("strong", { text: item.name }), el("small", { text: item.price })])
                ]));
            });
            recent.hidden = false;
        }
        const clear = $("[data-recent-clear]", recent);
        clear && clear.addEventListener("click", () => {
            store.set(RECENT_KEY, []);
            recent.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 250 }).onfinish = () => {
                recent.hidden = true;
            };
        });
    }

    /* ---------- Zoom ---------- */
    $$("[data-zoom]").forEach((zoom) => {
        const img = $("img", zoom);
        if (!img || !window.matchMedia("(pointer: fine)").matches) {
            return;
        }
        zoom.addEventListener("pointerenter", () => zoom.classList.add("is-zoom"));
        zoom.addEventListener("pointerleave", () => zoom.classList.remove("is-zoom"));
        zoom.addEventListener("pointermove", (event) => {
            if (event.target.closest(".fav-btn")) {
                zoom.classList.remove("is-zoom");
                return;
            }
            zoom.classList.add("is-zoom");
            const rect = zoom.getBoundingClientRect();
            zoom.style.setProperty("--zx", ((event.clientX - rect.left) / rect.width * 100).toFixed(1) + "%");
            zoom.style.setProperty("--zy", ((event.clientY - rect.top) / rect.height * 100).toFixed(1) + "%");
        });
    });

    /* ---------- Payment ---------- */
    const payForm = $("[data-pay-form]");
    if (payForm) {
        const card3d = $("[data-card3d]");
        const field = (name) => $("[data-pay='" + name + "']", payForm);
        const hint = (name, text, kind) => {
            const node = $("[data-hint='" + name + "']", payForm);
            if (node) {
                node.textContent = text || "";
                node.className = "field-hint" + (kind ? " " + kind : "");
            }
        };
        const digits = (value) => value.replace(/\D/g, "");
        const luhn = (number) => {
            let sum = 0;
            let double = false;
            for (let i = number.length - 1; i >= 0; i -= 1) {
                let d = Number(number[i]);
                if (double) {
                    d *= 2;
                    if (d > 9) {
                        d -= 9;
                    }
                }
                sum += d;
                double = !double;
            }
            return number.length >= 13 && sum % 10 === 0;
        };
        const brand = (number) => {
            if (/^4/.test(number)) {
                return "VISA";
            }
            if (/^(5[1-5]|2[2-7])/.test(number)) {
                return "Mastercard";
            }
            if (/^220[0-4]/.test(number)) {
                return "МИР";
            }
            return "nova pay";
        };
        const checks = {
            card() {
                const number = digits(field("card").value);
                if (!number) {
                    hint("card", "Введите номер карты", "bad");
                    return false;
                }
                if (!luhn(number)) {
                    hint("card", "Номер не проходит проверку Луна — проверьте цифры", "bad");
                    return false;
                }
                hint("card", "Номер корректный · " + brand(number), "good");
                return true;
            },
            exp() {
                const match = /^(\d{2})\/(\d{2})$/.exec(field("exp").value);
                if (!match) {
                    hint("exp", "Формат ММ/ГГ", "bad");
                    return false;
                }
                const month = Number(match[1]);
                const year = 2000 + Number(match[2]);
                const now = new Date();
                if (month < 1 || month > 12) {
                    hint("exp", "Месяц от 01 до 12", "bad");
                    return false;
                }
                if (year < now.getFullYear() || (year === now.getFullYear() && month < now.getMonth() + 1)) {
                    hint("exp", "Срок карты истёк", "bad");
                    return false;
                }
                hint("exp", "", "");
                return true;
            },
            cvc() {
                const ok = /^\d{3}$/.test(field("cvc").value);
                hint("cvc", ok ? "" : "3 цифры на обороте", ok ? "" : "bad");
                return ok;
            }
        };
        const render = () => {
            const number = digits(field("card").value);
            const groups = (number + "•".repeat(Math.max(0, 16 - number.length))).slice(0, 19).match(/.{1,4}/g) || [];
            $("[data-card-number]").textContent = groups.join(" ");
            $("[data-card-brand]").textContent = brand(number);
            $("[data-card-holder]").textContent = (field("holder").value || "ИМЯ ФАМИЛИЯ").toUpperCase();
            $("[data-card-exp]").textContent = field("exp").value || "ММ/ГГ";
            $("[data-card-cvc]").textContent = field("cvc").value.replace(/./g, "•") || "•••";
        };
        field("card").addEventListener("input", (event) => {
            const value = digits(event.target.value).slice(0, 19);
            event.target.value = value.replace(/(.{4})/g, "$1 ").trim();
            render();
            if (value.length >= 16) {
                checks.card();
            } else {
                hint("card", "");
            }
        });
        field("exp").addEventListener("input", (event) => {
            let value = digits(event.target.value).slice(0, 4);
            if (value.length >= 3) {
                value = value.slice(0, 2) + "/" + value.slice(2);
            }
            event.target.value = value;
            render();
            if (value.length === 5) {
                checks.exp();
            }
        });
        field("cvc").addEventListener("input", (event) => {
            event.target.value = digits(event.target.value).slice(0, 3);
            render();
        });
        field("holder").addEventListener("input", render);
        field("cvc").addEventListener("focus", () => card3d && card3d.classList.add("is-flipped"));
        field("cvc").addEventListener("blur", () => card3d && card3d.classList.remove("is-flipped"));
        render();

        payForm.addEventListener("submit", (event) => {
            if (payForm.dataset.ready === "true") {
                return;
            }
            event.preventDefault();
            const ok = [checks.card(), checks.exp(), checks.cvc()].every(Boolean) && field("holder").value.trim().length > 0;
            if (!ok) {
                payForm.animate([{ transform: "translateX(0)" }, { transform: "translateX(-8px)" }, { transform: "translateX(8px)" }, { transform: "translateX(0)" }], { duration: 320 });
                toast({ kind: "err", title: "Проверьте данные карты", text: "Поля с ошибкой подсвечены", life: 3000 });
                return;
            }
            const button = $("button[type='submit']", payForm);
            if (button) {
                button.disabled = true;
                button.classList.add("is-loading");
            }
            window.setTimeout(() => {
                payForm.dataset.ready = "true";
                if (button) {
                    button.disabled = false;
                }
                if (typeof payForm.requestSubmit === "function") {
                    payForm.requestSubmit();
                } else {
                    payForm.submit();
                }
            }, reducedMotion ? 0 : 1100);
        });
    }

    /* ---------- Confetti ---------- */
    const confettiHost = $("[data-confetti]");
    if (confettiHost && !reducedMotion) {
        const key = "nova-confetti-" + confettiHost.dataset.confetti;
        if (!store.get(key, false)) {
            store.set(key, true);
            const canvas = el("canvas", { class: "confetti", "aria-hidden": "true" });
            document.body.appendChild(canvas);
            const ctx = canvas.getContext("2d");
            const ratio = window.devicePixelRatio || 1;
            canvas.width = window.innerWidth * ratio;
            canvas.height = window.innerHeight * ratio;
            ctx.scale(ratio, ratio);
            const colors = ["#6a45ff", "#c9f24d", "#ff3b6b", "#8b6cff", "#12a36b", "#ffb020"];
            const pieces = Array.from({ length: 140 }, () => ({
                x: window.innerWidth / 2 + (Math.random() - 0.5) * 120,
                y: window.innerHeight * 0.35,
                vx: (Math.random() - 0.5) * 16,
                vy: -Math.random() * 14 - 4,
                w: 6 + Math.random() * 6,
                h: 8 + Math.random() * 10,
                r: Math.random() * Math.PI,
                vr: (Math.random() - 0.5) * 0.3,
                c: colors[Math.floor(Math.random() * colors.length)]
            }));
            const started = performance.now();
            const frame = (now) => {
                const elapsed = now - started;
                ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
                pieces.forEach((p) => {
                    p.vy += 0.35;
                    p.vx *= 0.99;
                    p.x += p.vx;
                    p.y += p.vy;
                    p.r += p.vr;
                    ctx.save();
                    ctx.globalAlpha = Math.max(0, 1 - elapsed / 2600);
                    ctx.translate(p.x, p.y);
                    ctx.rotate(p.r);
                    ctx.fillStyle = p.c;
                    ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
                    ctx.restore();
                });
                if (elapsed < 2600) {
                    window.requestAnimationFrame(frame);
                } else {
                    canvas.remove();
                }
            };
            window.requestAnimationFrame(frame);
        }
    }

    /* ---------- Forms ---------- */
    $$("[data-pw-toggle]").forEach((button) => {
        const input = $("input", button.closest(".pw"));
        button.addEventListener("click", () => {
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            button.setAttribute("aria-label", show ? "Скрыть пароль" : "Показать пароль");
            button.style.color = show ? "var(--brand)" : "";
            input.focus();
        });
    });

    $$("[data-strength-input]").forEach((input) => {
        const scope = input.closest(".field");
        const meter = $("[data-strength]", scope);
        const label = $("[data-strength-label]", scope);
        const names = ["", "Слабый пароль", "Так себе", "Хороший пароль", "Отличный пароль"];
        input.addEventListener("input", () => {
            const value = input.value;
            let score = 0;
            if (value.length >= 6) {
                score += 1;
            }
            if (value.length >= 10) {
                score += 1;
            }
            if (/[A-ZА-Я]/.test(value) && /[a-zа-я]/.test(value)) {
                score += 1;
            }
            if (/\d/.test(value) && /[^\w\s]|_/.test(value)) {
                score += 1;
            } else if (/\d/.test(value) && score < 4) {
                score += value.length >= 8 ? 1 : 0;
            }
            score = value ? Math.max(1, Math.min(4, score)) : 0;
            meter && (meter.dataset.score = String(score));
            label && (label.textContent = value.length > 0 && value.length < 6 ? "Нужно ещё " + (6 - value.length) + " симв." : names[score]);
        });
    });

    const loginForm = $("[data-login-form]");
    if (loginForm) {
        $$("[data-demo]", loginForm).forEach((button) => {
            button.addEventListener("click", () => {
                const email = $("[name='username']", loginForm);
                const pass = $("[name='password']", loginForm);
                email.value = button.dataset.demo;
                pass.value = button.dataset.demoPass;
                button.animate([{ transform: "scale(1)" }, { transform: "scale(.97)" }, { transform: "scale(1)" }], { duration: 200 });
                const submit = $("button[type='submit']", loginForm);
                submit.classList.add("is-loading");
                window.setTimeout(() => loginForm.submit(), reducedMotion ? 0 : 350);
            });
        });
    }

    $$("[data-counter]").forEach((area) => {
        const out = $("[data-counter-out]", area.closest(".field"));
        const update = () => {
            if (out) {
                out.textContent = area.value.length + " / " + area.maxLength;
            }
        };
        area.addEventListener("input", update);
        update();
    });

    /* ---------- KPI count-up ---------- */
    const counters = $$("[data-count]");
    if (counters.length && "IntersectionObserver" in window && !reducedMotion) {
        const parse = (text) => {
            const clean = text.replace(/[\s  ]/g, "");
            const match = /^(-?[\d]+)(?:,(\d+))?(.*)$/.exec(clean);
            if (!match) {
                return null;
            }
            return { value: Number(match[1] + "." + (match[2] || "0")), decimals: (match[2] || "").length, suffix: text.includes("₽") ? " ₽" : "" };
        };
        const io = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) {
                    return;
                }
                io.unobserve(entry.target);
                const node = entry.target;
                const info = parse(node.textContent.trim());
                if (!info) {
                    return;
                }
                const fmt = new Intl.NumberFormat("ru-RU", { minimumFractionDigits: info.decimals, maximumFractionDigits: info.decimals });
                node.dataset.value = "0";
                animateNumber(node, info.value, (v) => fmt.format(v) + info.suffix);
            });
        });
        counters.forEach((node) => io.observe(node));
    }
})();
