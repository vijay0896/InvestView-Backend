# Third-Party Services & Integrations Guide

This document outlines every external/third-party service required to build, launch, and scale the application from a **₹0/month MVP** to a **production-grade fintech platform**.

Every service below is selected with a **Zero Vendor Lock-In** philosophy—using standard protocols (SMTP, JDBC, S3 API, Docker) or Java Interfaces (`@ConditionalOnProperty`) so you can seamlessly switch providers via `application.yml` without rewriting business logic.

---

## 1. Quick Overview Matrix

| Category | Core Purpose | MVP (Free / Low-Cost) | Production Target (India/Global) | Easy Switch Strategy |
| :--- | :--- | :--- | :--- | :--- |
| **1. Market Data** | OHLC Candles & Stock Quotes | Yahoo Finance v8 API | Broker APIs (Dhan/Upstox/Angel) or TrueData | `MarketDataProvider` Interface |
| **2. Email** | OTPs, Resets, Welcome & Alerts | Brevo / Resend | Amazon SES (AWS) | Standard SMTP (`JavaMailSender`) |
| **3. SMS / OTP** | Phone Verification & 2FA | Email OTP / Firebase Auth | MSG91 / 2Factor.in / Twilio | `OtpProvider` Interface |
| **4. Managed Redis** | Shared Chart & Quote Cache | Upstash Redis / Redis Cloud | AWS ElastiCache / Self-Hosted Redis | Spring Cache (`@Cacheable`) |
| **5. Managed MySQL** | Primary 3NF Database | Aiven Free / Local Docker | AWS RDS / DigitalOcean Managed DB | Standard JDBC + Flyway |
| **6. Edge / CDN / SSL** | HTTPS, DDoS, Rate Limiting | Cloudflare (Free) | Cloudflare Pro / AWS CloudFront | DNS-level proxy (Zero code) |
| **7. App Hosting** | Frontend & Backend Deployment | Vercel (FE) + Render/Railway (BE) | AWS EC2/ECS / DigitalOcean / Hetzner | `Dockerfile` Containerization |
| **8. Observability** | Error Tracking & Uptime Alerts | Sentry (Free) + UptimeRobot | Datadog / Grafana + Prometheus | Spring Actuator + SLF4J |
| **9. Object Storage** | CSV/CAS Uploads & Avatars *(Phase 2)* | Cloudflare R2 (10GB Free) | Amazon S3 | AWS S3 SDK (R2 is S3-compatible) |
| **10. Payments** | Pro Subscriptions *(Phase 2)* | Test Sandbox Mode | Razorpay / Cashfree (India) | `PaymentGateway` Interface |

---

## 2. Detailed Service Breakdown

### 1. Market Data Provider (Core Engine)
* **Purpose:** Fetches `15m` and `1d` OHLC (Open, High, Low, Close) candlestick data for NSE/BSE stocks to construct the user's synthetic portfolio index.
* **MVP Option (Free):** **Yahoo Finance v8 Chart API** accessed via Spring `RestClient` with a 15-minute `@Scheduled` background sync.
* **Production Upgrade:**
  * *Connect-Broker Model:* **Upstox API**, **Angel One SmartAPI**, or **DhanHQ API** (via user OAuth login for real-time WebSocket ticks and auto-synced holdings).
  * *Direct Live Feed Model:* NSE-authorized vendors like **TrueData** or **Global Datafeeds (GDFL)**.
* **How to Inter-Switch:** Abstract all data fetching behind a `MarketDataProvider` Java interface.

---

### 2. Transactional Email Service
* **Purpose:** Sends account verification links, Email OTPs, password resets, security login alerts, and optional daily/weekly portfolio performance digests.
* **MVP Options (Free):**
  * **Brevo:** ~300 free emails/day (great for early traction).
  * **Resend:** ~3,000 free emails/month with a clean developer experience.
* **Production Upgrade:** **Amazon SES (AWS)** — lowest per-1,000 email cost at scale with high deliverability.
* **How to Inter-Switch:** Avoid vendor-specific SDKs. Use Spring's built-in `spring-boot-starter-mail` (`JavaMailSender`) over **SMTP**. Switching from Brevo to AWS SES only requires updating `host`, `port`, `username`, and `password` in `application.yml`.

---

### 3. SMS / Phone OTP Service
* **Purpose:** Verifies Indian mobile numbers during signup, enables 2FA for security, and sends critical portfolio breakout/stop-loss alerts.
* **MVP Options (Free):**
  * **Email OTP Fallback (Recommended for MVP):** ₹0 cost and avoids regulatory delays.
  * **Firebase Phone Auth:** Good for quick prototyping without initial DLT paperwork.
* **Production Upgrade (India):** **MSG91** or **2Factor.in** (significantly cheaper and more reliable for Indian telecom routes than Twilio).
* **India Compliance Note:** Sending commercial/OTP SMS in India requires **TRAI DLT Registration** (Entity ID + Sender Header + Approved Template IDs) regardless of the provider.
* **How to Inter-Switch:** Create an `OtpProvider` interface with implementations (`MockConsoleOtpProvider`, `EmailOtpProvider`, `Msg91OtpProvider`) toggled via `@ConditionalOnProperty(name = "app.otp.provider")`.

---

### 4. Managed Redis (Distributed Cache)
* **Purpose:** Stores computed portfolio candlestick JSONs (5–15 min TTL), hot stock quotes, and API rate-limit counters so MySQL is not overloaded during market hours.
* **MVP Options (Free):** **Upstash Redis** (serverless, generous free tier) or **Redis Cloud** (30MB free). For local development, Docker Redis or Spring's Caffeine cache works out of the box.
* **Production Upgrade:** **AWS ElastiCache** or a dedicated Redis instance on your cloud VPS.
* **How to Inter-Switch:** Use **Spring Data Redis** + **Spring Cache Abstraction** (`@Cacheable`, `@CacheEvict`). Switching only requires changing `spring.data.redis.url`.

---

### 5. Managed MySQL 8 Database
* **Purpose:** Stores normalized 3NF data (`users`, `user_credentials`, `user_addresses`, `stocks_master`, `portfolio_holdings`, `stock_candles`) using `UUIDv7` (`BINARY(16)`).
* **MVP Options (Free):** **Aiven** (Free MySQL tier), **TiDB Serverless** (MySQL-compatible), or a local **Docker MySQL 8** container during development.
* **Production Upgrade:** **AWS RDS for MySQL** or **DigitalOcean Managed MySQL** (automated daily backups, point-in-time recovery, and read replicas).
* **How to Inter-Switch:** Use standard JDBC + **Flyway** (or Liquibase) for versioned schema migrations so migrating databases is a single connection-string change.

---

### 6. Edge Security, DNS & CDN
* **Purpose:** Provides free SSL/TLS (HTTPS), hides your backend server IP, blocks DDoS/bot attacks, applies edge rate-limiting, and caches React static files globally.
* **MVP & Production:** **Cloudflare (Free Tier)** sitting in front of your domain and **Nginx** reverse proxy.
* **How to Inter-Switch:** Operates purely at the DNS/network layer—requires zero changes to Java or React code.

---

### 7. Application Hosting (Frontend & Backend)
* **Purpose:** Serves the React SPA and runs the stateless Spring Boot 3.x backend + scheduled market sync worker.
* **MVP Options (Free / Low-Cost):**
  * *Frontend (React):* **Vercel**, **Netlify**, or **Cloudflare Pages** (Free tier with instant Git deploys).
  * *Backend (Spring Boot):* **Render**, **Railway**, **Koyeb**, or **Oracle Cloud Free Tier**.
* **Production Upgrade:** **AWS EC2 / ECS**, **DigitalOcean App Platform**, or **Hetzner VPS** running Docker containers behind a load balancer.
* **How to Inter-Switch:** Package your Spring Boot app with a standard multi-stage `Dockerfile`. Any cloud provider can run the exact same container image.

---

### 8. Observability (Error Tracking & Uptime Monitoring)
* **Purpose:** Instantly notifies you if Yahoo Finance starts returning `429 Too Many Requests`, if a React chart crashes, or if the backend server goes down during market hours.
* **MVP Options (Free):**
  * **Sentry (Free Tier):** Captures stack traces and context for both React frontend and Spring Boot backend exceptions.
  * **UptimeRobot / Better Stack (Free Tier):** Pings `/actuator/health` every 5 minutes and alerts you via email/Telegram if the server is unreachable.
* **Production Upgrade:** **Datadog**, **New Relic**, or self-hosted **Prometheus + Grafana + Loki**.

---

### 9. Object Storage *(Phase 2 — Optional)*
* **Purpose:** Stores uploaded broker holdings CSVs (Zerodha/Groww), CAS PDFs for background parsing, or user profile avatars without filling up your backend server disk.
* **MVP Option (Free):** **Cloudflare R2** (10GB free storage, ₹0 bandwidth/egress charges).
* **Production Upgrade:** **Amazon S3**.
* **How to Inter-Switch:** Cloudflare R2 is 100% S3-API compatible. Use the standard AWS S3 Java SDK—switching between R2 and S3 only requires changing the endpoint URL and access keys.

---

### 10. Payment Gateway *(Phase 2 — Monetization)*
* **Purpose:** Handles recurring monthly/yearly subscriptions (e.g., Pro tier for live WebSocket charts, Nifty overlay, and unlimited holdings) via UPI, Cards, and NetBanking.
* **Production Options (India):** **Razorpay** or **Cashfree** (built-in UPI AutoPay and GST invoicing). Use **Stripe** if expanding globally.
* **How to Inter-Switch:** Isolate checkout creation and webhook verification inside a `PaymentGateway` interface.

---

## 3. Recommended "₹0 Cost" Launch Stack (Phase 1 MVP)

To validate your product with real investors without spending upfront capital, start with this exact combination:

1. **Market Data:** Yahoo Finance v8 API (15m delayed, cached in MySQL)
2. **Email & OTP:** Brevo SMTP (Free 300/day — handle OTP via Email initially)
3. **Database & Cache:** Aiven Free MySQL + Upstash Free Redis
4. **Hosting & Security:** Vercel (React) + Render/Railway (Spring Boot Docker) + Cloudflare (Free DNS/SSL)
5. **Monitoring:** Sentry (Free) + UptimeRobot (Free)