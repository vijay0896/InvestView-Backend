package com.investview.stock;

// Purpose: MySQL table stocks_master ka Java Object Representation (JPA Entity).
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity representing the NSE/BSE stock master list in MySQL.
 */
@Entity
@Table(name = "stock_master")

public class StockMaster {
    @Id
    @Column(name = "symbol", length = 25, nullable = false)
    private String symbol;// e.g. "RELIANCE.NS", "TCS.NS"

    @Column(name = "company_name", length = 150, nullable = false)
    private String companyName;// e.g. "Reliance Industries Ltd"

    @Column(name = "exchange", length = 10, nullable = false)
    private String exchange; // "NSE" or "BSE"

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Default no-arg constructor required by JPA/Hibernate.
     */
    public StockMaster() {
    }

    /**
     * Parameterized constructor for easy object creation.
     */
    public StockMaster(String symbol, String companyName, String exchange) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    // --- Getters and Setters ---
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}