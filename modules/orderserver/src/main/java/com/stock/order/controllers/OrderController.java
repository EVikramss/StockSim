package com.stock.order.controllers;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stock.models.StockData;
import com.stock.order.service.OrderCreationService;
import com.stock.order.types.UIOrder;
import com.stock.service.StockDataService;
import com.stock.service.StockListService;

@RestController
public class OrderController {

	@Autowired
	private StockListService stockListService;

	@Autowired
	private StockDataService stockDataService;

	@Autowired
	private OrderCreationService orderCreationService;

	/**
	 * List all stocks
	 * 
	 * @return
	 */
	@GetMapping(path = "/order/getListedStocks")
	public List<String> getListedStocksForOrder() {
		return stockListService.getAllListedStocks();
	}

	/**
	 * Get listed days for a stock
	 * 
	 * @param stockName
	 * @return
	 */
	@GetMapping(path = "/order/getListedDaysForStock")
	public List<String> getListedDaysForStock(@RequestParam String stockName) {
		return stockListService.getListedDaysForStock(stockName);
	}

	/**
	 * Get stock data for a particular day.
	 * 
	 * @param stockName
	 * @param day
	 * @return
	 */
	@GetMapping(path = "/order/getStockData")
	public List<StockData> getStockData(@RequestParam String stockName, @RequestParam String day) {
		return stockDataService.getStockData(stockName, day);
	}

	/**
	 * This method converts UI data, represented as a generic UIOrder to an actual
	 * order type
	 * 
	 * @param order
	 * @return
	 */
	@PostMapping("/order/submitBuyOrder")
	public List<String> receiveBuyOrder(@RequestBody UIOrder order) {
		try {
			orderCreationService.createBuyOrder(order);
		} catch (Exception e) {
			return Arrays.asList(new String[] { "failed" });
		}
		return Arrays.asList(new String[] { "success" });
	}
}
