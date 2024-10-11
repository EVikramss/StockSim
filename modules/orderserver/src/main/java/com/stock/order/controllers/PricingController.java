package com.stock.order.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stock.models.StockData;
import com.stock.service.PricingService;

@RestController
public class PricingController {

	@Autowired
	private PricingService pricingService;

	/**
	 * Controller to get price for a stock on a particular time of day.
	 * 
	 * @param stockName
	 * @param day
	 * @param time
	 * @param offset
	 * @return
	 */
	@GetMapping("/order/getPrice")
	public String[] getPrice(@RequestParam String stockName, @RequestParam String day, @RequestParam String hour,
			@RequestParam String minute, @RequestParam String minuteOffset) {
		List<StockData> data = pricingService.getStockData(stockName, day);

		// get current price
		String[] priceTimeVal = new String[] { "", "" };
		if (data != null && data.size() > 0)
			priceTimeVal = pricingService.getPrice(data, hour, minute, minuteOffset);

		return priceTimeVal;
	}
}
