package com.stock.order.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stock.order.service.BuyOrderTypesInfoService;

@RestController
public class OrderTypeController {
	
	@Autowired
	private BuyOrderTypesInfoService buyOrderTypesInfoService;

	@GetMapping("/order/buyOrderTypes")
	public List<Map<String, Object>> getBuyOrderTypes() {
		return buyOrderTypesInfoService.getTypesForUI();
	}
}
