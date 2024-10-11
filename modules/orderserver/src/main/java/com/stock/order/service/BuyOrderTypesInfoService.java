package com.stock.order.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.stock.common.Util;
import com.stock.order.interfaces.BuyOrder;
import com.stock.order.types.UIOrder;

@Service
public class BuyOrderTypesInfoService {

	@Autowired
	private ApplicationContext context;

	/**
	 * Get class and contructor parameter info for all buy orders.
	 * 
	 * @return
	 */
	public Map<String, Class<?>[]> getTypes() {
		return Arrays.asList(context.getBeanNamesForType(BuyOrder.class)).stream().map(l -> l.getClass())
				.collect(Collectors.toMap(c -> c.getName(), c -> c.getConstructors()[0].getParameterTypes()));
	}

	/**
	 * Return types for showing on UI.
	 * 
	 * @return
	 */
	public List<Map<String, Object>> getTypesForUI() {

		return Arrays.asList(context.getBeanNamesForType(BuyOrder.class)).stream().map(l -> context.getType(l))
				.map(c -> {

					String name = Util.getShortNameForOrderType(c.getName());
					Parameter[] params = c.getConstructors()[0].getParameters();
					List<String> paramNames = new ArrayList<String>();
					for (int i = 0; i < params.length; i++) {
						Parameter param = params[i];
						paramNames.add(param.getName());
					}

					Map<String, Object> orderTypeMap = new HashMap<String, Object>();
					orderTypeMap.put("orderType", name);
					orderTypeMap.put("fields", paramNames);

					return orderTypeMap;
				}).toList();
	}

	/**
	 * Convert UIOrder to the correct order type using reflection.
	 * 
	 * @param order
	 * @return
	 * @throws Exception
	 */
	public BuyOrder inferOrderType(UIOrder uiOrder) throws Exception {

		BuyOrder order = null;
		String[] beanNames = context.getBeanNamesForType(BuyOrder.class);

		for (String beanName : beanNames) {
			Class beanClass = context.getType(beanName);
			String name = Util.getShortNameForOrderType(beanClass.getName());

			if (name.equals(uiOrder.getOrderType())) {
				Object[] values = uiOrder.getValues().toArray();
				order = (BuyOrder) beanClass.getConstructors()[0].newInstance(values.length > 0 ? values : null);
				break;
			}
		}

		return order;
	}

}
