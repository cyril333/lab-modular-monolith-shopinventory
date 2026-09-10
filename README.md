# Modular Monolith Integration with a React Frontend

A Spring Boot backend with two in-process modules (`shop` / Order and `inventory` / Inventory) backed by Supabase (Postgres), connected to a React (Vite) frontend over REST.

## Project Structure
shopinventory-lab/
├── backend/ # Spring Boot app (edu.cit.antolijao.shop, edu.cit.antolijao.inventory)
├── frontend/ # React (Vite) app
├── sql/ # SQL script used to create/seed Supabase tables
└── screenshots/ # Network tab evidence (confirmed + rejected orders)


## Supabase Setup Steps

1. Create a free project at [supabase.com](https://supabase.com).
2. Go to the **SQL Editor** in the Supabase dashboard and run the script in [`sql/schema.sql`](sql/schema.sql) to create and seed the `inventory` and `orders` tables.
3. Go to **Project Settings → Database → Connection Pooling** and copy the **Session Pooler** connection details (host, port, username in the form `postgres.<project-ref>`). The Session Pooler is used here instead of the direct connection because the direct connection failed with a DNS resolution error on the network used for development (likely an IPv6-only routing issue).
4. Set your database password as an environment variable named `SUPABASE_DB_PASSWORD` — **never commit this value to the repo**. In PowerShell, before running the backend:
```powershell
   $env:SUPABASE_DB_PASSWORD="your-db-password"
```
5. `backend/src/main/resources/application.properties` references this environment variable directly:
```properties
   spring.datasource.password=${SUPABASE_DB_PASSWORD}
```
   No credentials are hardcoded anywhere in the repository.

## Running the Backend

```powershell
cd backend
$env:SUPABASE_DB_PASSWORD="your-db-password"
.\mvnw.cmd spring-boot:run
```

Runs on `http://localhost:8083`.

## Running the Frontend

```powershell
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

## API

**POST** `/api/orders`

Request:
```json
{ "productId": "P100", "quantity": 2 }
```

Response:
```json
{ "status": "CONFIRMED", "reason": null, "inventory": 23 }
```

## Network Tab Evidence

**Confirmed order (P100, sufficient stock):**

![Confirmed order](screenshots/confirmed-order.png)

**Rejected order (P300, 0 stock):**

![Rejected order](screenshots/rejected-order.png)

## Reflection

1. In-process versus microservices integration.

Running Order and Inventory as modules inside one Spring Boot process means calling InventoryService.reserve() is a normal Java method call. It happens at the time uses the same memory and shares one database transaction. If the order and the stock need to succeed or fail together one @Transactional boundary covers both. There is no network delay, no data conversion and no partial-failure state to deal with: either everything happens or nothing does.

If Inventory were its microservice, all of that easy behavior goes away. The call becomes a network request over HTTP or messaging which means it can fail the other service can be unreachable. The response can come late or in the wrong order. We would have to add mechanisms, timeouts and circuit breakers; a way to keep data in sync between two databases (like sagas or eventual consistency since one big transaction across two services isn’t possible); versioned API agreements between teams; and separate ways to run, watch and log each service. In short the in-process version gives consistency and simplicity without any effort. A microservices split gives up that ease for the ability to grow and release on its own.

2. Why package-private InventoryServiceImpl is important.

Making InventoryServiceImpl package-private (no keyword) means classes outside the inventory package. Including the shop package. Cannot use the class at compile time. The Order module must rely on the InventoryService interface, which is the correct and stable agreement between the two modules.

If InventoryServiceImpl was public there would be nothing stopping OrderService from using the implementation class skipping the interface. That would create a connection: any internal changes to InventoryServiceImpl (like renaming fields changing helper methods or how it talks to the database) could break the Order module even if the public interface stayed the same. Package-private visibility turns a rule (“only use Inventory through its interface”) into something the compiler ensures, instead of something that depends on people to follow it over time.

3. When to take Inventory. Make it its own microservice.

Taking Inventory out as its own microservice makes sense when Inventory needs to grow, release or change on its own. For example if inventory checks get more traffic than order placement if a separate team owns Inventory and needs to release on its own or if Inventory needs its own database or technology for performance.

To do it the single Java interface for InventoryService would need to be replaced with a network client (like REST or messaging) that matches the method names so OrderService still uses an interface but the code now makes an HTTP call instead of a direct method call. Inventory would need its database (no longer sharing the same Postgres instance or transaction with Order) its own service with health checks and logs and OrderService would need to manage network issues it never dealt with before. Like timeouts, retries and how to keep the order and inventory reservation in sync without a shared transaction (, like reserve-then-confirm or a saga/compensating action if the reservation works but the order fails later).
