package com.stock.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stock.models.StockData;

@Repository
public interface StockDataRepo extends JpaRepository<StockData, Long> {
	
	@Query("select distinct instrument, date from StockData")
	public List<Object[]> findDistinctStocksWithDate();

	@Query("select sd from StockData sd where sd.instrument = :stockName and sd.date = :day order by sd.time")
	public List<StockData> getStockDataForDay(@Param("stockName") String stockName, @Param("day") String day);
}
