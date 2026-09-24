# Anti-Corruption Layer (ACL) Integration Documentation

## 1. Supplier Catalog Mapping

### Primary Mapped Products
| Product ID | Product Name | Supplier SKU | PackSize | Notes |
| --- | --- | --- | --- | --- |
| **P100** | Wireless Mouse | NBR-3374 | 6 | Mapped & verified via PO-100028 |
| **P200** | Mechanical Keyboard | NBR-8036 | 6 | Mapped & verified via PO-100082 |
| **P300** | USB-C Hub | NBR-1384 | 10 | Mapped & verified via PO-100034 |

### Discovered Extra SKUs (Unmapped)
The following SKUs were discovered in the LegacySupply catalog during exploration but are currently unmapped in our application domain:
- `NBR-1713` (Cable)
- `NBR-1897` (SSD)
- `NBR-4827` (Headset)
- `NBR-6483` (Webcam)
- `NBR-2743` (Mousepad)
- `NBR-7070` (Charger)
- `NBR-5832` (Flash Drive)

---

## 2. Session Behavior & Auth Lifecycle

- **Authentication Method:** Session tokens are acquired via `POST /auth/login` and sent on every subsequent request via the `X-LS-Session` header.
- **Measured Session Lifetime:** Live testing showed valid session responses at 20 seconds and failure at 30 seconds. Effective session lifetime is treated as **~20 seconds**.
- **ACL Handling Strategy:**
  - `LegacySupplySession` encapsulates session retrieval and expiration logic.
  - Upon encountering an `E-AUTH-07` (HTTP 401) error, `LegacySupplyClient` invalidates the current session token, automatically acquires a fresh session token via `/auth/login`, and retries the failed operation transparently without bubbling errors up to the business domain.

---

## 3. Error Codes & Resilience Rules

| Code | HTTP Status | Meaning | Occurrence & Recovery Strategy |
| --- | --- | --- | --- |
| **E-AUTH-07** | 401 | Session invalid or expired | Triggered when session exceeds ~20s lifetime. Handled automatically via transparent token renewal. |
| **E-SYS-99** | 503 | Transient service outage | Chaos-injected failure. Handled via 3-attempt backoff loop (500ms → 1s → 2s). |
| **E-SYS-50** | 503 | Transient service outage variant | Handled via the same exponential backoff retry strategy as E-SYS-99. |
| **E-IDEM-04** | 409 | Request ID reused with different payload | Triggered when `X-Request-Id` is reused with mismatched payload body. Prevented by persisting and preserving the original `X-Request-Id` across retries. |

### Status Code Mapping & Unknown Code Policy
- **Known Code Mappings:**
  - `10` → `ACCEPTED`
  - `20` → `PICKING`
  - `30` → `SHIPPED`
  - `40` → `DELIVERED`
- **Unknown Status Code Policy:** Any unexpected status code received from LegacySupply is logged without throwing an exception, defaulting to `ACCEPTED` to prevent corrupting local state or interrupting poller execution.

---

## 4. Quantity / UoM Worked Examples

### Example 1: PO-100028 (P100 - Wireless Mouse)
- **Inventory Deficit Needed:** 20 units
- **Supplier PackSize:** 6 units/case
- **Conversion Math:** $\lceil 20 / 6 \rceil = 4$ cases (`CS`)
- **Payload Transmitted:** `Qty = 4`, `Uom = CS`
- **Units Received on Delivery:** $4 \times 6 = 24$ units restocked into `InventoryService`

### Example 2: PO-100082 (P200 - Mechanical Keyboard)
- **Inventory Deficit Needed:** 6 units
- **Supplier PackSize:** 6 units/case
- **Conversion Math:** $\lceil 6 / 6 \rceil = 1$ case (`CS`)
- **Payload Transmitted:** `Qty = 1`, `Uom = CS`
- **Units Received on Delivery:** $1 \times 6 = 6$ units restocked into `InventoryService`