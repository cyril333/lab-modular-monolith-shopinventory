DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS supplier_orders;

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