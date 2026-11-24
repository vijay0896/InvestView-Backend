package com.investview.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds initial NSE stock data on application startup if the table is empty.
 */
@Component
public class StockDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockDataSeeder.class);

    private final StockMasterRepository stockMasterRepository;

    // Constructor Injection (Dependency Inversion / Inversion of Control)
    public StockDataSeeder(StockMasterRepository stockMasterRepository) {
        this.stockMasterRepository = stockMasterRepository;
    }

    @Override
    public void run(String... args) {
        long count = stockMasterRepository.count();
        if (count == 0) {
            log.info("stocks_master table is empty. Seeding initial Nifty stocks...");

            List<StockMaster> initialStocks = List.of(
                new StockMaster("RELIANCE.NS", "Reliance Industries Ltd", "NSE"),
                new StockMaster("TCS.NS", "Tata Consultancy Services Ltd", "NSE"),
                new StockMaster("HDFCBANK.NS", "HDFC Bank Ltd", "NSE"),
                new StockMaster("INFY.NS", "Infosys Ltd", "NSE"),
                new StockMaster("ICICIBANK.NS", "ICICI Bank Ltd", "NSE"),
                new StockMaster("HINDUNILVR.NS", "Hindustan Unilever Ltd", "NSE"),
                new StockMaster("ITC.NS", "ITC Ltd", "NSE"),
                new StockMaster("SBIN.NS", "State Bank of India", "NSE"),
                new StockMaster("BHARTIARTL.NS", "Bharti Airtel Ltd", "NSE"),
                new StockMaster("KOTAKBANK.NS", "Kotak Mahindra Bank Ltd", "NSE"),
                new StockMaster("LT.NS", "Larsen & Toubro Ltd", "NSE"),
                new StockMaster("TATAMOTORS.NS", "Tata Motors Ltd", "NSE"),
                new StockMaster("BAJFINANCE.NS", "Bajaj Finance Ltd", "NSE"),
                new StockMaster("WIPRO.NS", "Wipro Ltd", "NSE"),
                new StockMaster("ASIANPAINT.NS", "Asian Paints Ltd", "NSE")
            );

            stockMasterRepository.saveAll(initialStocks);
            log.info("Successfully seeded {} stocks into stocks_master!", initialStocks.size());
        } else {
            log.info("stocks_master table already contains {} stocks. Skipping seeder.", count);
        }
    }
}