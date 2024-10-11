package com.stock.order.controllers;

import static java.util.Map.entry;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class OrderUIPageController {

	/**
	 * Show index page
	 * 
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, path = "/index")
	public String showUI() {
		return "stockList";
	}

	/**
	 * Show stock details page
	 * 
	 * @param stockName
	 * @param day
	 * @return
	 */
	@GetMapping(path = "stockPage")
	public ModelAndView showStockPage(@RequestParam(name = "name") String stockName, @RequestParam String day) {
		ModelAndView mav = new ModelAndView("stockPage",
				Map.ofEntries(entry("stockName", stockName), entry("day", day)));
		return mav;
	}
}
