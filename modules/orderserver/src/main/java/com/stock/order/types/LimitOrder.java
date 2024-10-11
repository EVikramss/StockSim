package com.stock.order.types;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.stock.order.interfaces.BuyOrder;

@Component
@Lazy
public class LimitOrder implements BuyOrder {

	private float limitValueF;

	public LimitOrder(String limitValue) {
		this.limitValueF = Float.parseFloat(limitValue);
	}

	@Override
	public String toString() {
		return "LimitOrder " + limitValueF;
	}
}