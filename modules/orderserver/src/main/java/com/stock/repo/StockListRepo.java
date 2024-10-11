package com.stock.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stock.models.StockName;

@Repository
public interface StockListRepo extends JpaRepository<StockName, String> {

	@Query("select name from StockName")
	public List<String> findAllEntries();
}
