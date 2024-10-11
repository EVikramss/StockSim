package com.stock.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stock.order.interfaces.BuyOrder;
import com.stock.order.types.UIOrder;
import com.stock.topic.TopicPublisher;

@Service
public class OrderCreationService {

	@Autowired
	private BuyOrderTypesInfoService buyOrderTypesInfoService;
	
	@Autowired
	private TopicPublisher topicPublisher;

	/**
	 * Use service to infer order type and return it.
	 * 
	 * @param order
	 * @return
	 * @throws Exception 
	 */
	public BuyOrder createBuyOrder(UIOrder order) throws Exception {
		BuyOrder buyOrder = buyOrderTypesInfoService.inferOrderType(order);
		topicPublisher.sendMessage("IN.ORDER.BUY", buyOrder.toString());
		return buyOrder;
	}
}
