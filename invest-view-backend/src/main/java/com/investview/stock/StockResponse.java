package com.investview.stock;

/**
 * Immutable DTO representing a stock search result for the frontend.
 */

public record StockResponse(
        String symbol,
        String companyName,
        String exchange,
        Integer  isActive ) {

    /**
     * Factory method to map a JPA Entity to a clean DTO.
     */

    public static StockResponse fromEntity(StockMaster stock) {
        return new StockResponse(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getExchange(),
                stock.getIsActive() ? 1 : 0
            
            );
    }

}
