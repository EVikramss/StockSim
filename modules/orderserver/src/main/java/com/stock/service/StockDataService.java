package com.stock.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stock.models.StockData;
import com.stock.models.StockDay;
import com.stock.models.StockName;
import com.stock.repo.StockDataRepo;
import com.stock.repo.StockListRepo;

import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StockDataService {

	@Autowired
	private StockDataRepo stockDataRepo;

	@Autowired
	private StockListRepo stockListRepo;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Get current entry count
	 * 
	 * @return
	 */
	public long getCount() {
		return stockDataRepo.count();
	}

	/**
	 * Save entire list to db, flush and clear context.
	 * 
	 * @param data
	 */
	@Transactional
	public void saveAll(List<StockData> data) {
		stockDataRepo.saveAllAndFlush(data);
		// clear the persistent context to free memory
		entityManager.clear();
	}

	/**
	 * Once stock data is loaded, refresh the following dependent tables - 1.
	 * StockName 2. StockDay
	 * 
	 */
	@Timed(value = "StockDataService.clearAndRefreshRelatedTables")
	public void clearAndRefreshRelatedTables() {
		// get distinct combination of instrument & date from raw data tables
		List<Object[]> currentStockList = stockDataRepo.findDistinctStocksWithDate();

		// clear existing data in main lookup tables
		stockListRepo.deleteAll();

		// convert raw data to stockName and days map
		Map<StockName, Set<StockDay>> newStockMap = getConsolidatedMap(currentStockList);

		// convert map to list after setting stockDay
		List<StockName> newStockList = getListFromMap(newStockMap);

		// assign primary keys to stock days
		updatePrimaryKeyForDays(newStockList);

		// save data to DB
		stockListRepo.saveAllAndFlush(newStockList);

		// clear the persistent context to free memory from 1st level(persistent
		// context) cache
		entityManager.clear();
	}

	/**
	 * Method to convert consolidated map to list. Normalized to record timings.
	 * Access /actuator/prometheus
	 * 
	 * @param newStockMap
	 * @return
	 */
	@Timed(value = "StockDataService.getListFromMap")
	private List<StockName> getListFromMap(Map<StockName, Set<StockDay>> newStockMap) {
		// associate stock name with each day as part of bi directional mapping
		// & return list of stocks
		List<StockName> newStockList = newStockMap.keySet().stream().map(k -> {
			Set<StockDay> days = newStockMap.get(k);
			days.forEach(d -> d.setStockName(k));
			k.setDays(days);
			return k;
		}).toList();

		return newStockList;
	}

	/**
	 * Get consolidated map from object array. Normalized to record timings.
	 * 
	 * @param currentStockList
	 * @return
	 */
	@Timed(value = "StockDataService.getConsolidatedMap")
	private Map<StockName, Set<StockDay>> getConsolidatedMap(List<Object[]> currentStockList) {
		Map<StockName, Set<StockDay>> newStockMap = currentStockList.stream()
				.collect(Collectors.toMap(obj -> new StockName((String) obj[0]),
						obj -> new HashSet<>(Arrays.asList(new StockDay((String) obj[1]))), (exs, newval) -> {
							exs.addAll(newval);
							return exs;
						}));
		return newStockMap;
	}

	/**
	 * Update primary keys for stock - days so that they are globally unique
	 * 
	 * @param newStockList
	 */
	@Timed(value = "StockDataService.updatePrimaryKeyForDays")
	private void updatePrimaryKeyForDays(List<StockName> newStockList) {
		
		AtomicLong acounter = new AtomicLong();
		newStockList.stream().flatMap(s -> s.getDays().stream()).forEach(d -> {
			d.setId(acounter.incrementAndGet());
		});
	}
	
	/**
	 * Returns a particular stocks data for given day
	 * 
	 * @param stockName
	 * @param day
	 * @return
	 */
	public List<StockData> getStockData(String stockName, String day) {
		return stockDataRepo.getStockDataForDay(stockName, day);
	}
}
