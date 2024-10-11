package com.stock.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stock.models.StockData;
import com.stock.repo.StockDaysRepo;
import com.stock.repo.StockListRepo;

import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StockListService {

	@Autowired
	private StockListRepo stockListRepo;
	
	@Autowired
	private StockDaysRepo stockDaysRepo;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Get all listed stocks
	 * 
	 * @return
	 */
	@Timed("StockListService.getAllListedStocks")
	public List<String> getAllListedStocks() {
		return stockListRepo.findAllEntries();
	}
	
	/**
	 * 
	 * Get listed days for a selected stock.
	 * @param stockName
	 * @return
	 */
	@Timed("StockListService.getListedDaysForStock")
	public List<String> getListedDaysForStock(String stockName) {
		return stockDaysRepo.getListedDaysForStock(stockName);
	}

	
}
