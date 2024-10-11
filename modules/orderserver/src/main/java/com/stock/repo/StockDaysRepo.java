package com.stock.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stock.models.StockDay;

@Repository
public interface StockDaysRepo extends JpaRepository<StockDay, Long> {

	@Query(value = "select stock_date from Stock_Day where stock_Name_name = :stockName order by stock_date", nativeQuery = true)
	public List<String> getListedDaysForStock(@Param("stockName") String stockName);
}
