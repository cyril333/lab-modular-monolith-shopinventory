-- Drop existing tables (clean recreate, per lab instructions)
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;

-- Inventory table (unchanged from Lab 1)
CREATE TABLE inventory (
    product_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    stock INTEGER NOT NULL
);

-- Orders table: status now supports CONFIRMED, REJECTED, CANCELLED
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    status TEXT NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Order items: one order can now have multiple line items
CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(order_id),
    product_id TEXT NOT NULL REFERENCES inventory(product_id),
    quantity INTEGER NOT NULL
);

-- Notifications: written by the Notification module's event listeners
CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed data (same as Lab 1)
INSERT INTO inventory (product_id, name, stock) VALUES
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0);