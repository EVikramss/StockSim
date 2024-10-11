package com.stock.data.providers;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * This class helps select the appropriate provider for fetching data
 */
@Service
@RequiredArgsConstructor
public class DataProviderFactoryService {

	@Autowired
	private List<DataProvider> providers;

	/**
	 * This method filters providers based on a csv filter passed and returns the
	 * first matching provider.
	 * 
	 * @param filter
	 * @return
	 */
	public DataProvider getProvider(String filter) {
		return providers.stream().filter(p -> p.getType().containsAll(Arrays.asList(filter.split(",")))).findFirst()
				.get();
	}
}
