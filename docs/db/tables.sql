USE invest_view_db;

-- ========================================================
-- 1. PARENT TABLE: users (User Profile Domain)
-- ========================================================
CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    date_of_birth DATE NULL COMMENT 'Derived age, never hardcode age',
    email VARCHAR(150) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 2. CHILD TABLE: user_credentials (1:1 Security Isolation)
-- ========================================================
CREATE TABLE IF NOT EXISTS user_credentials (
    user_id BINARY(16) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    last_login_at TIMESTAMP NULL,
    failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_credentials PRIMARY KEY (user_id),
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) 
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 3. CHILD TABLE: user_addresses (1:N Address Mapping)
-- ========================================================
CREATE TABLE IF NOT EXISTS user_addresses (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    country VARCHAR(50) NOT NULL DEFAULT 'India',
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_addresses PRIMARY KEY (id),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) 
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_addresses_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 4. PARENT TABLE: stocks_master (NSE/BSE Static Master List)
-- ========================================================
CREATE TABLE IF NOT EXISTS stocks_master (
    symbol VARCHAR(25) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    exchange VARCHAR(10) NOT NULL COMMENT 'NSE / BSE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stocks_master PRIMARY KEY (symbol),
    INDEX idx_stocks_master_company (company_name),
    INDEX idx_stocks_master_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 5. CHILD TABLE: portfolio_holdings (User Stock Holdings)
-- ========================================================
CREATE TABLE IF NOT EXISTS portfolio_holdings (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    symbol VARCHAR(25) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    avg_buy_price DECIMAL(18, 4) NOT NULL,
    buy_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_portfolio_holdings PRIMARY KEY (id),
    CONSTRAINT fk_portfolio_holdings_user FOREIGN KEY (user_id) 
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_holdings_stock FOREIGN KEY (symbol) 
        REFERENCES stocks_master (symbol) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- Ek user ek stock ka ek hi holding record rakhega (Duplicate Prevention)
    CONSTRAINT uq_user_holding_symbol UNIQUE (user_id, symbol),
    INDEX idx_holdings_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 6. TIME-SERIES TABLE: stock_candles (Market Data Cache)
-- ========================================================
CREATE TABLE IF NOT EXISTS stock_candles (
    symbol VARCHAR(25) NOT NULL,
    timeframe VARCHAR(10) NOT NULL COMMENT '15m / 1d',
    candle_time BIGINT NOT NULL COMMENT 'UNIX epoch seconds for TradingView',
    open_price DECIMAL(18, 4) NOT NULL,
    high_price DECIMAL(18, 4) NOT NULL,
    low_price DECIMAL(18, 4) NOT NULL,
    close_price DECIMAL(18, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Clustered B-Tree Composite Index for fast range queries
    CONSTRAINT pk_stock_candles PRIMARY KEY (symbol, timeframe, candle_time),
    CONSTRAINT fk_stock_candles_stock FOREIGN KEY (symbol) 
        REFERENCES stocks_master (symbol) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;