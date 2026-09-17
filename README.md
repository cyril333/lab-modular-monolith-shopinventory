# Modular Monolith: Order/Inventory + Notifications

Spring Boot backend with three in-process modules (`shop`, `inventory`, `notification`) sharing one Supabase Postgres database, plus a React (Vite) frontend.

## Stack
Java Spring Boot, React (Vite), Supabase (Postgres)

## Setup

### 1. Supabase
1. Create a Supabase project.
2. Run `sql/schema.sql` in the SQL Editor (drops and recreates all tables + seed data).
3. Get your **Session Pooler** connection string (Direct connection failed via IPv6 on our network) — Connect button → Session pooler.

### 2. Backend
```bash
cd backend
$env:SUPABASE_DB_PASSWORD="your-password"   # PowerShell, per session
./mvnw spring-boot:run
```
Runs on `http://localhost:8083`. Update `application.properties` datasource URL/username to match your own Supabase pooler string.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

## Event listeners: sync or async?
Listeners run **synchronously** (default `@EventListener`, not `@Async`). We kept it synchronous because the lab's scale is trivial (a few DB writes) and synchronous listeners guarantee the notification is persisted before the HTTP response returns — simpler to reason about and test. The tradeoff: if `NotificationListener` ever failed or ran slowly, it would currently block/fail the whole `placeOrder()` transaction, since it's on the same call stack. `@Async` would decouple that at the cost of losing the guarantee that a notification exists immediately after order placement.

## Screenshots
See `screenshots/`:
- `confirmed.png` — all items succeed
- `rejected.png` — one item fails, whole order REJECTED, no partial reservation
- `cancel-restock.png` — inventory reflecting restock after cancel
- `confirmed-rejected-lowstock.png` — confirmed + rejected + low-stock together

## Reflection

**1.**

In-process, atomicity comes from two things working together: the validation-before-reservation pattern (every item is checked against current stock *before* any `reserve()` call happens, so a mid-loop failure can't leave partial state), and Spring's `@Transactional` wrapping the whole `placeOrder()` method in one database transaction — if anything throws, the DB rolls back everything, including any `order_items` rows already inserted. Crucially, `InventoryService.reserve()` and the `orders`/`order_items` writes all share the same JDBC connection and transaction, so a partial failure can't leave inventory decremented without a matching order record. If Order and Inventory were separate services communicating over a network, this guarantee disappears — a network call can fail after the request left but before a response returns, and two independent databases can't share one ACID transaction. We'd need either a **saga pattern** (each step publishes an event; a failure triggers compensating actions, e.g., a "release reservation" call to undo a partial reserve) or a **two-phase commit/distributed transaction coordinator**, plus idempotency keys so retries don't double-reserve stock, and likely an outbox pattern to guarantee the "reserve succeeded" event is reliably published even if the service crashes right after committing.

**2.**

Publishing via `ApplicationEventPublisher` means `OrderService` has zero compile-time or runtime dependency on `NotificationListener` — it doesn't import anything from the `notification` package, doesn't know it exists, and doesn't care if zero or five things are listening. This is the inversion: instead of Order calling Notification, Notification subscribes to what Order publishes. If `NotificationListener` were deleted entirely, `OrderService` would compile and run identically, just with no notifications created — that's the boundary enforcement the lab is testing. If Notification became a separate microservice, this loose coupling would need to survive a process boundary: in-process Spring events don't cross the network, so we'd need a **message broker** (RabbitMQ, Kafka, or similar) that Order publishes to and Notification consumes from, plus **delivery guarantees** (at-least-once delivery, with Notification made idempotent since the same event might be redelivered), and likely a **transactional outbox** so the event is only published if the order transaction actually committed (otherwise we'd risk notifying about an order that got rolled back).

**3.**

I'd extract **Inventory** first. It's the most self-contained: it owns its own data (`inventory` table), has a narrow, stable interface (`getItem`, `reserve`, `restock`), and is the module most likely to need independent scaling (stock lookups are read-heavy and could benefit from caching or read replicas, separately from order-processing load). Notification is arguably *easier* to extract technically (it's already fully decoupled via events), but Inventory extraction is more valuable because it directly protects the highest-consequence data (nobody wants overselling from a scaling bottleneck). To do it, `InventoryService`'s interface would become a REST (or gRPC) client instead of an in-process interface — `OrderService` would call an HTTP endpoint instead of an autowired bean, we'd lose the shared-transaction guarantee described in Q1 and need a saga/compensating-action for rollback, and `InventoryServiceImpl` would move into its own deployable with its own database, no longer sharing the same schema or connection pool as `shop`/`notification`.