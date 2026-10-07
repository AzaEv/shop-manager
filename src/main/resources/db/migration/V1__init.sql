CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT NOT NULL REFERENCES categories (id),
    name         VARCHAR(200) NOT NULL,
    sku          VARCHAR(64) NOT NULL UNIQUE,
    description  TEXT,
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock        INTEGER NOT NULL CHECK (stock >= 0),
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE customers (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(180) NOT NULL UNIQUE,
    name           VARCHAR(180) NOT NULL,
    phone          VARCHAR(40),
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE carts (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL UNIQUE REFERENCES customers (id)
);

CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products (id),
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customers (id),
    status       VARCHAR(20) NOT NULL,
    total        NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    address      VARCHAR(500) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    BIGINT REFERENCES products (id),
    product_name  VARCHAR(200) NOT NULL,
    sku           VARCHAR(64) NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE TABLE order_status_history (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20) NOT NULL,
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);
