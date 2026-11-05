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
        if (cartNote && !cartNote.contains(event.target) && !event.target.closest("form.buy")) {
            closeCartNote();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            setCatalog(false);
            setAccount(false);
            closeCartNote();
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

    let cartNote = null;

    function closeCartNote() {
        if (!cartNote) {
            return;
        }
        cartNote.remove();
        cartNote = null;
    }

    function showCartNote(title, name, detail, withCartLink) {
        closeCartNote();
        const note = document.createElement("aside");
        note.className = "cart-note" + (withCartLink ? "" : " is-error");
        note.setAttribute("role", withCartLink ? "status" : "alert");

        const close = document.createElement("button");
        close.type = "button";
        close.className = "cart-note-close";
        close.setAttribute("aria-label", "Закрыть");
        close.textContent = "×";
        close.addEventListener("click", closeCartNote);

        const kicker = document.createElement("p");
        kicker.className = "kicker";
        kicker.textContent = title;

        note.appendChild(close);
        note.appendChild(kicker);
        if (name) {
            const strong = document.createElement("strong");
            strong.textContent = name;
            note.appendChild(strong);
        }
        const line = document.createElement("p");
        line.className = "muted";
        line.textContent = detail;
        note.appendChild(line);
        if (withCartLink) {
            const link = document.createElement("a");
            link.className = "btn sm full";
            link.href = "/cart";
            link.textContent = "Открыть корзину";
            note.appendChild(link);
        }
        document.body.appendChild(note);
        cartNote = note;
    }

    function cartUnits(cart) {
        return (cart.items || []).reduce(function (sum, item) {
            return sum + Number(item.quantity || 0);
        }, 0);
    }

    function rub(value) {
        const amount = Number(value);
        if (!Number.isFinite(amount)) {
            return "";
        }
        return new Intl.NumberFormat("ru-RU", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(amount) + " ₽";
    }

    function setCartCount(count) {
        document.querySelectorAll("[data-cart-link]").forEach(function (link) {
            let badge = link.querySelector(".badge");
            if (count < 1) {
                if (badge) {
                    badge.remove();
                }
                return;
            }
            if (!badge) {
                badge = document.createElement("span");
                badge.className = "badge";
                const label = link.querySelector(".icon-label");
                if (label) {
                    link.insertBefore(badge, label);
                } else {
                    link.appendChild(badge);
                }
            }
            badge.textContent = String(count);
            badge.classList.remove("is-pop");
            void badge.offsetWidth;
            badge.classList.add("is-pop");
        });
    }

    document.querySelectorAll("form.buy").forEach(function (form) {
        let ticket = 0;
        form.addEventListener("submit", function (event) {
            if (typeof window.fetch !== "function") {
                return;
            }
            event.preventDefault();
            const button = form.querySelector("button[type='submit']");
            const productInput = form.querySelector("[name='productId']");
            const quantityInput = form.querySelector("[name='quantity']");
            const productId = Number(productInput && productInput.value);
            const quantity = Number(quantityInput && quantityInput.value);
            const productName = form.getAttribute("data-product-name") || "Товар";
            const idleLabel = "В корзину";
            if (!productId || quantity < 1) {
                showCartNote("Не добавлено", "", "Укажите количество больше нуля", false);
                return;
            }
            const mine = ++ticket;
            if (button) {
                button.disabled = true;
                button.classList.add("is-loading");
                button.textContent = "Добавляем…";
            }
            window.fetch("/api/cart/items", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({ productId: productId, quantity: quantity })
            }).then(function (response) {
                const type = response.headers.get("content-type") || "";
                if (!type.includes("json")) {
                    throw new Error("Сессия истекла. Войдите снова и повторите.");
                }
                return response.json().then(function (body) {
                    if (!response.ok) {
                        throw new Error(body.message || "Не удалось добавить товар");
                    }
                    return body;
                });
            }).then(function (cart) {
                const count = cartUnits(cart);
                const line = (cart.items || []).find(function (item) {
                    return Number(item.productId) === productId;
                });
                const inCart = line ? line.quantity : quantity;
                setCartCount(count);
                showCartNote(
                    "Добавлено в корзину",
                    productName,
                    inCart + " шт. этого товара · итого " + rub(cart.total),
                    true
                );
                if (button) {
                    button.textContent = "Добавлено";
                }
            }).catch(function (error) {
                showCartNote("Не добавлено", productName, error.message || "Не удалось добавить товар", false);
                if (button && ticket === mine) {
                    button.textContent = idleLabel;
                }
            }).finally(function () {
                if (!button || ticket !== mine) {
                    return;
                }
                button.disabled = false;
                button.classList.remove("is-loading");
                window.setTimeout(function () {
                    if (ticket === mine) {
                        button.textContent = idleLabel;
                    }
                }, 1200);
            });
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
