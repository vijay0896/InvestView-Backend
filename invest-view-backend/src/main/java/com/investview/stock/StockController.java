package com.investview.stock;

import com.investview.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller providing public endpoints for searching stocks.
 */
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * Autocomplete search API for stocks.
     * E.g. GET /api/v1/stocks/search?q=tata
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StockResponse>>> searchStocks(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {

        List<StockResponse> results = stockService.searchStocks(query);
        return ResponseEntity.ok(ApiResponse.success("Stocks fetched successfully", results));
    }
}