package com.stock.order.types;

import java.util.List;

public class UIOrder {

	private String orderType;
	private List<String> fields;
	private List<String> values;

	public List<String> getFields() {
		return fields;
	}

	public List<String> getValues() {
		return values;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
}
