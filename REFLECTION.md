# Reflection

### 1. Reorder Storage and Retry Trigger
When LegacySupply experienced a transient service outage and rejected BuyerRef `RO-TEST-001`, the purchase order was saved locally in the `supplier_orders` database table with a `PENDING` status. The `SupplierOrderRetryJob` scheduled task queried for `PENDING` orders every 60 seconds and re-executed `attemptSend()`. It reused the exact stored `requestId` and `buyerRef` without regenerating them, successfully placing `PO-100028` once the upstream service recovered.

### 2. Session Lifetime & Renewal Strategy
Measured log outputs showed session tokens expiring between 20s and 30s, establishing an effective session lifetime of ~20 seconds. The `LegacySupplySession` component manages the authentication lifecycle by catching `401 Unauthorized` responses (`E-AUTH-07`), invalidating the expired token, fetching a new session token via `/auth/login`, and reissuing the failed request automatically.

### 3. Pack Size & Case Conversion Example
For an inventory deficit of 20 units of P100 (Wireless Mouse, PackSize 6), the Anti-Corruption Layer calculated the quantity in cases using `Math.ceil(20.0 / 6)`, resulting in `4` cases (`CS`) sent in the XML payload (`PO-100028`). Upon delivery, 4 cases delivered back 24 total units to `InventoryService.restock()`.