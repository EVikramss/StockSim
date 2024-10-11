package com.stock.order.types;

import org.springframework.stereotype.Component;

import com.stock.order.interfaces.BuyOrder;

@Component
public class MarketBuyOrder implements BuyOrder {

	public MarketBuyOrder() {
	}
	
	@Override
	public String toString() {
		return "MarketOrder";
	}
}
