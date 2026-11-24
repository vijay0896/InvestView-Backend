package com.investview.stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data JPA Repository for stocks_master table.
 */
@Repository
public interface StockMasterRepository extends JpaRepository<StockMaster, String> {
    /**
     * Case-insensitive autocomplete search by symbol or company name.
     * E.g. query "rel" matches "RELIANCE.NS" or "Reliance Industries Ltd".
     */
    @Query("SELECT s FROM StockMaster s WHERE s.isActive = true AND (" +
           "LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<StockMaster> searchStocks(@Param("query") String query);
}
