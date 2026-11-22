# Portfolio Candle — Live Candlestick Charting for Your Stock Portfolio

**Portfolio Candle** *(working title)* is a web application built for active stock market investors and swing traders. Instead of viewing a portfolio as a simple static number or a flat line graph, this platform transforms a user's entire stock holdings into a **Custom Portfolio Index (Synthetic ETF)** with interactive, near-live **OHLC (Open, High, Low, Close) Candlestick Charts**.

---

## 1. Problem Statement & Vision

Most retail broking platforms (Zerodha, Groww, Angel One, etc.) provide candlestick charts for *individual* stocks, while showing only a basic end-of-day line chart or simple P&L percentage for the *overall portfolio*. 

Active investors and traders face three main gaps:
1. **Hidden Intraday Volatility:** Investors cannot see how high or low their overall portfolio moved during a trading session—only where it closed.
2. **No Technical Analysis on Portfolios:** Traders cannot apply support, resistance, or trendlines to their combined basket of stocks.
3. **Multi-Broker Fragmentation:** Investors holding stocks across multiple brokers lack a unified visual chart of their combined performance.

**Our Solution:** Allow investors to search stocks, enter their buying price and quantity, and instantly visualize their combined portfolio's live/near-live performance as a candlestick chart—both in absolute value (₹) and percentage returns (%).

---

## 2. Core Features

### Phase 1: MVP (Minimum Viable Product)
* **Instant Stock Search:** Fast autocomplete search across NSE/BSE listed stocks.
* **Portfolio Holdings Manager:** Add, edit, or delete stocks with average buy price and quantity.
* **Portfolio Candlestick Chart:** Interactive OHLC candlestick view supporting multiple timeframes (`15m`, `1h`, `1d`).
* **Dual View Modes:** Toggle between **Absolute Portfolio Value (₹)** and **Net Return (%)**.
* **Near-Live Auto-Refresh:** Automatic data updates every 5–15 minutes during Indian market hours (9:15 AM – 3:30 PM IST).

### Phase 2: Planned Enhancements
* **Benchmark Overlay:** Compare portfolio candlesticks against a Nifty 50 (`^NSEI`) trendline.
* **CSV / Holdings Import:** Upload broker holding sheets (Zerodha/Groww CSV) to eliminate manual entry friction.
* **Time-Weighted Return (TWR) / NAV Engine:** Adjust candles accurately when new capital is added or withdrawn mid-timeline.
* **Guest Mode:** Let new users test the chart via browser `localStorage` before creating an account.

---

## 3. Tech Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Backend** | Java 17/21, Spring Boot 3.x, Maven | REST APIs, portfolio math engine, scheduled data sync |
| **Database** | MySQL 8.x (Spring Data JPA / Hibernate) | Persistent storage for stocks, holdings, and cached candles |
| **Caching** | Caffeine Cache (In-Memory) | Fast retrieval of computed charts and hot stock data |
| **Market Data** | Yahoo Finance Chart API (via `RestClient`) | Free 5m/15m delayed OHLC data for NSE/BSE (`.NS` / `.BO`) |
| **Frontend** | React.js | Responsive Single Page Application (SPA) |
| **State & API** | Redux Toolkit (RTK Query) | API caching, automatic polling, and state synchronization |
| **Charting** | TradingView `lightweight-charts` | Smooth, professional-grade financial candlestick rendering |

---

## 4. Technical Approach & System Architecture

To ensure the application stays fast, scalable, and free from rate-limiting/IP bans while using Yahoo Finance data, we follow a **Centralized Caching & Aggregation Architecture**:

### A. Static Stock Master List
* Instead of hitting external APIs for every search keystroke, we maintain a `stocks_master` table in MySQL containing NSE/BSE symbols (e.g., `RELIANCE.NS`, `TCS.NS`) and company names. This delivers instant search results.

### B. Centralized Background Data Sync (Preventing Rate Limits)
* The frontend **never** calls Yahoo Finance directly, nor does the backend fetch data on every user page load.
* A Spring `@Scheduled` background worker runs every **15 minutes** strictly during Indian market hours (`9:15 AM – 3:30 PM IST, Monday–Friday`).
* The worker fetches `SELECT DISTINCT symbol` from all user holdings, pulls the latest OHLC candles from Yahoo Finance, and upserts them into MySQL (`stock_candles_cache`).

### C. On-Demand Portfolio Aggregation
* When a user requests their portfolio chart, the Spring Boot service fetches their holdings and the corresponding cached candles from MySQL/Caffeine, computes the combined OHLC values per timestamp, and returns TradingView-compatible JSON (`time`, `open`, `high`, `low`, `close`).

### D. Reactive Frontend with RTK Query
* RTK Query manages all API calls with `pollingInterval` set to 15 minutes during market hours.
* Adding or updating a stock triggers cache tag invalidation (`invalidatesTags: ['PortfolioChart']`), immediately recalculating and re-rendering the candlestick chart.

---

## 5. Database Schema Overview (MySQL)

1. **`stocks_master`**
   * `id` (PK), `symbol` (UNIQUE, e.g., `INFY.NS`), `company_name`, `exchange` (`NSE`/`BSE`)
2. **`portfolio_holdings`**
   * `id` (PK), `user_id`, `symbol` (FK), `quantity` (DECIMAL), `avg_buy_price` (DECIMAL), `created_at`, `updated_at`
3. **`stock_candles_cache`**
   * `id` (PK), `symbol`, `timeframe` (`15m`, `1d`), `candle_timestamp` (TIMESTAMP), `open_price`, `high_price`, `low_price`, `close_price`
   * *Composite Unique Index:* `(symbol, timeframe, candle_timestamp)`

---

## 6. Core Mathematical Logic

For a portfolio consisting of $N$ stocks, where each stock $i$ has quantity $Q_i$ and average buy price $B_i$:

### 1. Total Invested Capital
$$I_{\text{total}} = \sum_{i=1}^{N} (Q_i \times B_i)$$

### 2. Portfolio OHLC at Timestamp $t$
For each candle interval timestamp $t$, the portfolio's absolute value OHLC is calculated as:

$$\text{Open}_t = \sum_{i=1}^{N} (O_{i,t} \times Q_i)$$

$$\text{High}_t = \sum_{i=1}^{N} (H_{i,t} \times Q_i)$$

$$\text{Low}_t = \sum_{i=1}^{N} (L_{i,t} \times Q_i)$$

$$\text{Close}_t = \sum_{i=1}^{N} (C_{i,t} \times Q_i)$$

### 3. Net Return Percentage Mode (%)
To render the candlestick chart in terms of overall portfolio return percentage, each OHLC value $V_t \in \{\text{Open}_t, \text{High}_t, \text{Low}_t, \text{Close}_t\}$ is converted as:

$$\text{Return\%}(V_t) = \left( \frac{V_t - I_{\text{total}}}{I_{\text{total}}} \right) \times 100$$

---

## 7. Planned REST API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/stocks/search?q={query}` | Search NSE/BSE stocks by name or symbol |
| `GET` | `/api/v1/portfolio/holdings` | Fetch all holdings for the current user |
| `POST` | `/api/v1/portfolio/holdings` | Add a new stock holding (symbol, qty, buy price) |
| `PUT` | `/api/v1/portfolio/holdings/{id}` | Update quantity or average buy price |
| `DELETE` | `/api/v1/portfolio/holdings/{id}` | Remove a stock from the portfolio |
| `GET` | `/api/v1/portfolio/chart?timeframe=15m&mode=PERCENT` | Returns aggregated OHLC JSON array for TradingView |

---

## 8. Development Roadmap

* **Milestone 1 (Backend Foundation):** Spring Boot + Maven setup, MySQL schema creation, NSE stock master seed script, and CRUD APIs for holdings.
* **Milestone 2 (Data & Math Engine):** Yahoo Finance `RestClient` integration, `@Scheduled` 15-minute sync worker, and Portfolio OHLC aggregation service.
* **Milestone 3 (Frontend Integration):** React + RTK Query setup, stock autocomplete search UI, holdings table, and TradingView `lightweight-charts` integration.
* **Milestone 4 (Testing & Launch):** Market-hours testing, mobile responsiveness, Nifty 50 overlay, and beta deployment.

---

## 9. Disclaimer

Market data fetched via Yahoo Finance is delayed (5–15 minutes) and intended strictly for educational, prototyping, and personal portfolio visualization purposes. For commercial high-frequency production scaling, the data layer is designed to be easily swapped with authorized broker APIs (e.g., Upstox, Dhan, Kite Connect) or NSE-authorized data vendors.# InvestView-Backend
