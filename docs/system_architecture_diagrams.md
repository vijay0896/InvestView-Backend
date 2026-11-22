# System Architecture & Technical Design Specification

This document serves as the single source of truth for the **Portfolio Candle** application's architecture, database schema, and data flows. It is structured to be easily parsed by developers and AI coding agents (e.g., Antigravity) to generate consistent, production-ready code without over-engineering.

---

## 1. Cloud & System Architecture (Modular Monolith)

### Overview
The system follows a **Stateless Modular Monolith** architecture using **Spring Boot 3.x (Java 21 + Maven)** on the backend and **React.js + Redux Toolkit (RTK Query)** on the frontend. It is designed to run cost-effectively on a single node for the MVP while remaining horizontally scalable via stateless JWT authentication, shared Redis caching, and ShedLock distributed scheduling.

### Layer-by-Layer Breakdown

1. **Client Layer (Web Browser):**
   * **React.js + Tailwind CSS:** Renders the responsive dashboard, instant stock autocomplete, and holdings management UI.
   * **TradingView Lightweight Charts:** Renders high-performance interactive OHLC candlestick charts (`15m`, `1h`, `1d`) and percentage return overlays.
   * **RTK Query:** Handles API state management, client-side caching, tag-based invalidation (`invalidatesTags: ['PortfolioChart']` on mutations), and automatic 15-minute background polling during Indian market hours.

2. **Edge & Ingress Layer (Nginx / Cloudflare):**
   * Handles SSL/TLS termination, reverse proxying, load balancing across backend instances, and edge-level rate limiting/DDoS protection.

3. **Application Layer (Spring Boot 3.x — Stateless):**
   * **Spring Security (JWT) + REST Controllers:** Validates stateless JWT tokens and routes requests to the appropriate business modules.
   * **Auth, User & Portfolio Service:** Manages user registration, credentials, addresses, stock master search, and portfolio holdings CRUD. Triggers an on-demand cold-start fetch if a newly added stock has no historical candles yet.
   * **Portfolio OHLC Engine:** Aggregates individual stock OHLC candles into a single synthetic portfolio index using `BigDecimal` precision. Implements **Forward-Fill (Carry Forward)** logic to prevent false chart crashes when illiquid stocks miss a candle at a specific timestamp.
   * **Background Market Sync (`@Scheduled` + `ShedLock`):** Runs every 15 minutes strictly during Indian market hours (`9:15 AM – 3:30 PM IST, Mon–Fri`). Queries `SELECT DISTINCT symbol` from active holdings and uses **Spring `RestClient` + Resilience4j** (Retry + CircuitBreaker) to fetch batch OHLC data from Yahoo Finance.

4. **Data & Infrastructure Layer:**
   * **Redis Cache:** Shared distributed cache storing computed portfolio charts (5–15m TTL) and hot stock quotes.
   * **MySQL 8 (HikariCP):** Primary relational database normalized to 3NF, using time-ordered **UUIDv7 (`BINARY(16)`)** primary keys for optimal InnoDB B-Tree insert performance.

5. **Observability:**
   * **Spring Boot Actuator + Structured Logging:** Exposes `/actuator/health` and metrics to monitor API latency, DB connection pools, and Yahoo Finance fetch failures.

### Eraser.io Code (Cloud Architecture)
```eraser
// --- 1. CLIENT & EDGE LAYER ---
Client [icon: monitor, color: blue, label: "Web Client (Browser)"] {
  ReactUI [icon: react, label: "React.js + Tailwind + TradingView Charts"]
  RTK [icon: redux, label: "RTK Query (State & 15m Polling)"]
}

Edge [icon: shield, color: cyan, label: "Edge & Ingress (Nginx / Cloudflare)"] {
  Gateway [icon: globe, label: "SSL Termination, Load Balancer & Rate Limiting"]
}

// --- 2. APPLICATION LAYER (STATELESS MODULAR MONOLITH) ---
SpringBoot [icon: java, color: green, label: "Spring Boot 3.x (Java 21 + Maven) - Stateless"] {
  
  SecurityAPI [icon: lock, label: "Spring Security (JWT) + REST Controllers"]
  
  Modules [color: teal, label: "Core Business Modules"] {
    UserPortfolio [icon: briefcase, label: "Auth, User & Portfolio Service"]
    OHLCEngine [icon: cpu, label: "Portfolio OHLC Engine (Math + Forward-Fill)"]
  }
  
  SyncWorker [color: orange, label: "Background Market Sync"] {
    Scheduler [icon: clock, label: "@Scheduled (15m IST) + ShedLock"]
    YahooClient [icon: cloud, label: "RestClient + Resilience4j (Retry/CircuitBreaker)"]
  }
}

// --- 3. DATA & INFRASTRUCTURE LAYER ---
DataLayer [color: purple, label: "Persistence & Caching"] {
  Redis [icon: zap, color: red, label: "Redis Cache (Computed Charts & Stock Quotes)"]
  MySQL [icon: mysql, color: purple, label: "MySQL 8 (3NF, UUIDv7, HikariCP Pool)"]
}

// --- 4. EXTERNAL & OBSERVABILITY ---
YahooAPI [icon: globe, color: red, label: "Yahoo Finance v8 Chart API"]
Monitoring [icon: activity, color: gray, label: "Observability (Spring Actuator + Logs)"]

// --- CLEAN LEFT-TO-RIGHT FLOWS ---
ReactUI > RTK: User Actions & Chart Render
RTK > Gateway: HTTPS REST (JSON + JWT)
Gateway > SecurityAPI: Routed Requests

SecurityAPI > UserPortfolio: Auth, Search & Holdings
SecurityAPI > OHLCEngine: GET /portfolio/chart

UserPortfolio > MySQL: Read/Write Users & Holdings
UserPortfolio > YahooClient: Cold-Start Fetch (New Stock)

OHLCEngine > Redis: 1. Check Cached Chart (15m TTL)
OHLCEngine > MySQL: 2. Cache Miss (Batch OHLC Query)

Scheduler > MySQL: Fetch Distinct Active Symbols
Scheduler > YahooClient: Trigger 15m Batch Sync
YahooClient > YahooAPI: HTTP GET (15m / 1d OHLC)
YahooClient > MySQL: Bulk Upsert Candles
YahooClient > Redis: Refresh / Evict Cache

SpringBoot > Monitoring: Health, Metrics & Error Logs
```

---

## 2. Entity Relationship Diagram (ERD — MySQL 8)

### Core DBA Principles Applied
* **UUIDv7 (`BINARY(16)`):** All user-facing entities (`users`, `user_credentials`, `user_addresses`, `portfolio_holdings`) use time-ordered UUIDv7 stored as `BINARY(16)` (16 bytes) instead of `VARCHAR(36)` to prevent InnoDB index fragmentation while maintaining global uniqueness.
* **Derived Attribute Rule:** `date_of_birth` is stored instead of `age` so user age never becomes stale.
* **Security Isolation (1:1 Shared PK):** `user_credentials` shares `user_id` as both Primary Key and Foreign Key with `users`, isolating `password_hash` from standard profile queries.
* **Financial Precision:** All prices and quantities strictly use `DECIMAL(18,4)` in MySQL and `BigDecimal` in Java—never `FLOAT` or `DOUBLE`.
* **Time-Series Optimization (`stock_candles`):** Uses a **Composite Primary Key `(symbol, timeframe, candle_time)`** instead of a surrogate UUID, preventing duplicate candles and making range queries (`WHERE symbol IN (...) AND candle_time >= ...`) execute in milliseconds. A weekly cleanup job purges `15m` candles older than 60 days while retaining `1d` candles indefinitely.

### Eraser.io Code (Entity Relationship)
```eraser
// --- USER & AUTH DOMAIN ---
users [icon: user, color: blue] {
  id binary(16) pk "UUIDv7"
  full_name varchar(120) not null
  date_of_birth date "Age is derived, never stored"
  email varchar(150) unique not null
  phone_number varchar(15) unique not null
  is_active boolean "default true"
  created_at timestamp
  updated_at timestamp
}

user_credentials [icon: lock, color: red] {
  user_id binary(16) pk, fk "UUIDv7 - 1:1 Shared PK with users"
  password_hash varchar(255) not null
  last_login_at timestamp
  failed_attempts tinyint "default 0"
  updated_at timestamp
}

user_addresses [icon: map-pin, color: teal] {
  id binary(16) pk "UUIDv7"
  user_id binary(16) fk "UUIDv7"
  address_line1 varchar(255) not null
  address_line2 varchar(255)
  city varchar(100) not null
  state varchar(100) not null
  pincode varchar(10) not null
  country varchar(50) "default India"
  is_primary boolean "default true"
  created_at timestamp
  updated_at timestamp
}

// --- PORTFOLIO & MARKET DOMAIN ---
stocks_master [icon: list, color: green] {
  symbol varchar(25) pk "e.g. RELIANCE.NS"
  company_name varchar(150) not null
  exchange varchar(10) not null "NSE / BSE"
  is_active boolean "default true"
}

portfolio_holdings [icon: briefcase, color: orange] {
  id binary(16) pk "UUIDv7"
  user_id binary(16) fk "UUIDv7"
  symbol varchar(25) fk
  quantity decimal(18,4) not null
  avg_buy_price decimal(18,4) not null
  buy_date date
  created_at timestamp
  updated_at timestamp
}

stock_candles [icon: bar-chart, color: purple] {
  symbol varchar(25) pk, fk "Composite PK"
  timeframe varchar(10) pk "15m / 1d"
  candle_time bigint pk "UNIX Seconds"
  open_price decimal(18,4) not null
  high_price decimal(18,4) not null
  low_price decimal(18,4) not null
  close_price decimal(18,4) not null
}

// --- RELATIONSHIPS ---
users.id - user_credentials.user_id
users.id < user_addresses.user_id
users.id < portfolio_holdings.user_id
stocks_master.symbol < portfolio_holdings.symbol
stocks_master.symbol < stock_candles.symbol
```

---

## 3. Sequence Diagram (Write & Read Flows)

### Flow Breakdown
1. **Flow 1 — Holding Mutation & Cold-Start Sync (Write Path):**
   * The user adds a stock (`symbol`, `quantity`, `avg_buy_price`) via React.
   * Spring Boot validates the JWT, saves the holding in `portfolio_holdings`, and checks if `stock_candles` already has data for that symbol.
   * **Cold-Start Handling:** If no candles exist yet, Spring Boot immediately fetches historical `15m` and `1d` candles from Yahoo Finance and bulk-upserts them into MySQL so the user does not have to wait 15 minutes for the next cron cycle.
   * Spring Boot evicts the user's cached chart in Redis and returns `201 Created`. RTK Query automatically invalidates its local cache tag and triggers Flow 2.

2. **Flow 2 — Portfolio Chart Rendering (Read Path — Cache-Aside Pattern):**
   * React requests `GET /api/v1/portfolio/chart?timeframe=15m&mode=PERCENT`.
   * Spring Boot checks **Redis** first.
   * On a **Cache Miss**, it fetches the user's holdings and performs a single batch query on `stock_candles` (`WHERE symbol IN (...)`).
   * The `PortfolioOHLCEngine` computes the aggregated OHLC candles (applying Forward-Fill for missing timestamps), caches the JSON result in Redis (15-minute TTL), and returns the TradingView-formatted array (`[{ time, open, high, low, close }]`).

### Eraser.io Code (Sequence Diagram)
```eraser
User [icon: user]
React [icon: react, label: "React + RTK Query"]
Spring [icon: java, label: "Spring Boot API (JWT)"]
Redis [icon: zap, label: "Redis Cache"]
MySQL [icon: mysql, label: "MySQL 8"]
Yahoo [icon: globe, label: "Yahoo Finance API"]

// Flow 1: User adds a new stock (Write Path)
User > React: Enter Stock, Qty, Buy Price
React > Spring: POST /api/v1/portfolio/holdings (JWT)
Spring > MySQL: Insert into portfolio_holdings (UUIDv7)
Spring > MySQL: Check if candles exist in stock_candles

alt [label: "Cold Start (New Stock Symbol)"] {
  Spring > Yahoo: On-demand fetch (15m & 1d OHLC)
  Yahoo --> Spring: OHLC JSON Response
  Spring > MySQL: Bulk Upsert into stock_candles
}

Spring > Redis: Evict User Chart Cache
Spring --> React: 201 Created (RTK invalidatesTags)

// Flow 2: Chart Rendering (Read Path)
React > Spring: GET /api/v1/portfolio/chart?tf=15m&mode=PERCENT
Spring > Redis: Check Cached Portfolio Candles

alt [label: "Cache Miss"] {
  Spring > MySQL: Fetch User Holdings + Batch Fetch Stock Candles
  MySQL --> Spring: Raw OHLC Data
  Spring > Spring: Run PortfolioOHLCEngine (BigDecimal Sum + Forward-Fill)
  Spring > Redis: Store Computed Candles (TTL 15m)
}

Spring --> React: TradingView JSON [{time, open, high, low, close}]
React > User: Render Live Candlestick Chart
```

---

## 4. Implementation Guardrails for AI Coding Agents (Antigravity)

When generating code for this repository, strictly adhere to the following rules:
1. **Java & Maven:** Use Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security (JWT), and `com.github.f4b6a3:uuid-creator` (`UuidCreator.getTimeOrderedEpoch()`) for UUIDv7 generation.
2. **Database Types:** Map UUIDs to `BINARY(16)` in MySQL. Map all financial values (`quantity`, `avg_buy_price`, `open_price`, `high_price`, `low_price`, `close_price`) to `BigDecimal` in Java and `DECIMAL(18,4)` in MySQL.
3. **External API Resilience:** Never call Yahoo Finance from the frontend or on every chart read. Always route Yahoo Finance calls through a dedicated `YahooFinanceClient` using Spring `RestClient` with a standard browser `User-Agent` header and Resilience4j `@Retry` / `@CircuitBreaker`.
4. **Distributed Locking:** Annotate all `@Scheduled` market sync jobs with `@SchedulerLock` (ShedLock) to prevent duplicate execution when scaling to multiple backend instances.
5. **TradingView Format:** The `/api/v1/portfolio/chart` endpoint must return `time` as UNIX epoch seconds (`long`), sorted strictly in ascending order with no duplicate timestamps.