DROP TABLE IF EXISTS tiangge_order_items;
DROP TABLE IF EXISTS tiangge_orders;
DROP TABLE IF EXISTS channel_cursor;
DROP TABLE IF EXISTS supplier_orders;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;

CREATE TABLE inventory (
    product_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    stock INTEGER NOT NULL
);

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id),
    product_id TEXT NOT NULL REFERENCES inventory(product_id),
    quantity INTEGER NOT NULL
);

CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_orders (
    id SERIAL PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES inventory(product_id),
    buyer_ref TEXT NOT NULL UNIQUE,
    request_id TEXT NOT NULL UNIQUE,
    po_number TEXT,
    cases INTEGER,
    units INTEGER NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

INSERT INTO inventory (product_id, name, stock) VALUES
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0);

CREATE TABLE channel_cursor (
    id INTEGER PRIMARY KEY,
    last_seq BIGINT NOT NULL DEFAULT 0
);
INSERT INTO channel_cursor (id, last_seq) VALUES (1, 0);

CREATE TABLE tiangge_orders (
    tiangge_order_id TEXT PRIMARY KEY,
    shop_order_id BIGINT,
    decision TEXT,
    cancel_confirmed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE tiangge_order_items (
    id SERIAL PRIMARY KEY,
    tiangge_order_id TEXT NOT NULL REFERENCES tiangge_orders(tiangge_order_id),
    product_id TEXT NOT NULL,
    qty INTEGER NOT NULL
);