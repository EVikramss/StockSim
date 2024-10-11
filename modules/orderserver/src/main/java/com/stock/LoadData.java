package com.stock;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.stock.data.providers.DataProvider;
import com.stock.data.providers.DataProviderFactoryService;
import com.stock.models.StockData;
import com.stock.service.StockDataService;

import jakarta.annotation.PostConstruct;

/**
 * 
 * This class is used to load data into DB.
 * 
 */
@SpringBootApplication
public class LoadData {
	
	public static String csvFilePath = "C:\\Users\\sriemani\\Downloads\\BNF_2010_2020.csv";
	public static String serpaipFileDir = "C:\\Users\\sriemani\\Downloads\\serp";

	@Autowired
	private DataProviderFactoryService dataProviderService;

	@Autowired
	private StockDataService stockDataService;

	public static void main(String[] args) {
		SpringApplication.run(LoadData.class, args);
	}

	/**
	 * Load listed stock lists into database
	 */
	@PostConstruct
	private void load() {
		// get last key in table
		Long key = stockDataService.getCount() + 1;

		// get provider for history data and save to db
		DataProvider provider = dataProviderService.getProvider("HISTORY,CSV");
		List<StockData> data = provider.getData(key);
		stockDataService.saveAll(data);
		
		// get provider for current data and save to db
		provider = dataProviderService.getProvider("CURRENT");
		data = provider.getData(key);
		stockDataService.saveAll(data);

		// update related tables
		stockDataService.clearAndRefreshRelatedTables();
		
		System.exit(0);
	}
}
