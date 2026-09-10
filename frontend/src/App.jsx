import { useState } from 'react';
import './App.css';

const PRODUCTS = [
  { id: 'P100', name: 'Wireless Mouse' },
  { id: 'P200', name: 'Mechanical Keyboard' },
  { id: 'P300', name: 'USB-C Hub' },
];

function App() {
  const [productId, setProductId] = useState(PRODUCTS[0].id);
  const [quantity, setQuantity] = useState(1);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    setLoading(true);

    try {
      const response = await fetch('http://localhost:8083/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, quantity: Number(quantity) }),
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <h1>Place an Order</h1>

      <form onSubmit={handleSubmit} className="order-form">
        <label>
          Product
          <select value={productId} onChange={(e) => setProductId(e.target.value)}>
            {PRODUCTS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.id} — {p.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Quantity
          <input
            type="number"
            min="1"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            required
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Placing order...' : 'Submit Order'}
        </button>
      </form>

      <div className="result-area">
        {error && <p className="error">Error: {error}</p>}
        {result && (
          <div className={`result ${result.status === 'CONFIRMED' ? 'confirmed' : 'rejected'}`}>
            <p><strong>Status:</strong> {result.status}</p>
            {result.reason && <p><strong>Reason:</strong> {result.reason}</p>}
            <p><strong>Remaining Inventory:</strong> {result.inventory}</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;