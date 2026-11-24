package com.investview.stock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of StockService handling business logic and transactions.
 */
@Service
@Transactional(readOnly = true)

public class StockServiceImpl implements StockService {
    private final StockMasterRepository stockMasterRepository;
    // Constructor Injection (Best Practice)
    public StockServiceImpl(StockMasterRepository stockMasterRepository) {
        this.stockMasterRepository = stockMasterRepository;
    }
    @Override
    public List<StockResponse> searchStocks(String query) {
        // Agar query empty ya whitespace ho, toh saare active stocks return karo
        if (query == null || query.trim().isEmpty()) {
            return stockMasterRepository.findAll().stream()
                    .filter(StockMaster::getIsActive)
                    .map(StockResponse::fromEntity)
                    .toList();
        }
        // Database me LIKE '%query%' search chalao
        return stockMasterRepository.searchStocks(query.trim()).stream()
                .map(StockResponse::fromEntity)
                .toList();
    }
}