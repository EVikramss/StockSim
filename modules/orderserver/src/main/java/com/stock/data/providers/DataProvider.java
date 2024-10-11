package com.stock.data.providers;

import java.util.List;

import com.stock.models.StockData;

public interface DataProvider {

	public List<StockData> getData(Long startingKey);
	public List<String> getType();
}
