import { useState, useEffect } from 'react';
import './App.css';

const PRODUCTS = [
  { id: 'P100', name: 'Wireless Mouse' },
  { id: 'P200', name: 'Mechanical Keyboard' },
  { id: 'P300', name: 'USB-C Hub' },
];

const LOW_STOCK_THRESHOLD = 5;
const API_BASE = 'http://localhost:8083/api';

function App() {
  const [cart, setCart] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(PRODUCTS[0].id);
  const [selectedQuantity, setSelectedQuantity] = useState(1);

  const [inventory, setInventory] = useState([]);
  const [orders, setOrders] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function refreshAll() {
    try {
      const [invRes, ordersRes, notifRes] = await Promise.all([
        fetch(`${API_BASE}/inventory`),
        fetch(`${API_BASE}/orders`),
        fetch(`${API_BASE}/notifications`),
      ]);
      setInventory(await invRes.json());
      setOrders(await ordersRes.json());
      setNotifications(await notifRes.json());
    } catch (err) {
      console.error('Failed to refresh data', err);
    }
  }

  useEffect(() => {
    refreshAll();
  }, []);

  function addToCart() {
    if (!selectedProduct || selectedQuantity < 1) return;
    setCart((prev) => [...prev, { productId: selectedProduct, quantity: Number(selectedQuantity) }]);
    setSelectedQuantity(1);
  }

  function removeFromCart(index) {
    setCart((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmitOrder() {
    if (cart.length === 0) return;
    setError(null);
    setResult(null);
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items: cart }),
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const data = await response.json();
      setResult(data);
      setCart([]);
      await refreshAll();
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(orderId) {
    try {
      const response = await fetch(`${API_BASE}/orders/${orderId}/cancel`, { method: 'POST' });
      if (!response.ok) {
        throw new Error(`Cancel failed with status ${response.status}`);
      }
      await refreshAll();
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <div className="app">
      <h1>Shop &amp; Inventory</h1>

      <section className="cart-section">
        <h2>Build an Order</h2>
        <div className="cart-builder">
          <select value={selectedProduct} onChange={(e) => setSelectedProduct(e.target.value)}>
            {PRODUCTS.map((p) => (
              <option key={p.id} value={p.id}>{p.id} — {p.name}</option>
            ))}
          </select>
          <input
            type="number"
            min="1"
            value={selectedQuantity}
            onChange={(e) => setSelectedQuantity(e.target.value)}
          />
          <button type="button" onClick={addToCart}>Add to Cart</button>
        </div>

        {cart.length > 0 && (
          <ul className="cart-list">
            {cart.map((item, i) => (
              <li key={i}>
                {item.productId} × {item.quantity}
                <button type="button" onClick={() => removeFromCart(i)}>Remove</button>
              </li>
            ))}
          </ul>
        )}

        <button
          type="button"
          className="submit-order-btn"
          disabled={cart.length === 0 || loading}
          onClick={handleSubmitOrder}
        >
          {loading ? 'Placing order...' : 'Submit Order'}
        </button>

        <div className="result-area">
          {error && <p className="error">Error: {error}</p>}
          {result && (
            <div className={`result ${result.status === 'CONFIRMED' ? 'confirmed' : 'rejected'}`}>
              <p><strong>Status:</strong> {result.status}</p>
              {result.reason && <p><strong>Reason:</strong> {result.reason}</p>}
              <ul>
                {result.items.map((item, i) => (
                  <li key={i}>{item.productId} — {item.productName}: {item.outcome}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </section>

      <section className="inventory-section">
        <h2>Inventory</h2>
        <table>
          <thead>
            <tr><th>Product ID</th><th>Name</th><th>Stock</th></tr>
          </thead>
          <tbody>
            {inventory.map((item) => (
              <tr key={item.productId} className={item.stock < LOW_STOCK_THRESHOLD ? 'low-stock' : ''}>
                <td>{item.productId}</td>
                <td>{item.name}</td>
                <td>{item.stock}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="orders-section">
        <h2>Order History</h2>
        <ul className="order-list">
          {orders.map((order) => (
            <li key={order.orderId} className={`order-item order-${order.status.toLowerCase()}`}>
              <strong>Order {order.orderId}</strong> — {order.status}
              {order.reason && <span> ({order.reason})</span>}
              <ul>
                {order.items.map((item) => (
                  <li key={item.orderItemId}>{item.productId} × {item.quantity}</li>
                ))}
              </ul>
              {order.status === 'CONFIRMED' && (
                <button type="button" onClick={() => handleCancel(order.orderId)}>Cancel</button>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="notifications-section">
        <h2>Activity Feed</h2>
        <ul className="notification-list">
          {notifications.map((n) => (
            <li key={n.notificationId}>{n.message}</li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export default App;