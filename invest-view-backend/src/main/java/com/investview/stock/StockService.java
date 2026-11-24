package com.investview.stock;
import java.util.List;



/**
 * Service contract defining operations for searching and viewing stocks.
 */

public interface StockService {
    /**
     * Searches stocks by symbol or company name.
     * If query is empty, returns all active stocks.
     *
     * @param query search keyword
     * @return list of matching stocks
     */
    List<StockResponse> searchStocks(String query);
}