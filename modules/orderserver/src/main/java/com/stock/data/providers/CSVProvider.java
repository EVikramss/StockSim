package com.stock.data.providers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.stock.LoadData;
import com.stock.common.Util;
import com.stock.models.StockData;

/**
 * This class is used to read from csv historical data file
 */
@Component
public class CSVProvider implements DataProvider {
	
	Logger logger = LogManager.getLogger(CSVProvider.class);

	// register type as history provider
	private List<String> type = Arrays.asList("HISTORY", "CSV");

	/**
	 * Loop over data and set primary key as startingKey + index of current element
	 * in list
	 */
	@Override
	public List<StockData> getData(Long startingKey) {
		List<StockData> data = new ArrayList<StockData>();
		try {
			// load file data into String list
			List<String> lines = Files.readAllLines(Paths.get(LoadData.csvFilePath));
			String[] header = lines.get(0).split(",");

			// stream over list and add to output data
			data = LongStream.range(startingKey + 1, startingKey + lines.size()).parallel().mapToObj(i -> {
				String[] values = lines.get((int) (i - startingKey)).split(",");
				StockData stockdata = new StockData();
				// data.add(stockdata);

				for (int j = 0; j < values.length; j++) {
					String headerVal = header[j];
					String val = values[j];
					stockdata.setId(i - 1);

					switch (headerVal) {
					case "Instrument":
						stockdata.setInstrument(val);
						break;
					case "Date":
						stockdata.setDate(val);
						break;
					case "Time":
						stockdata.setTime(val);
						break;
					case "Open":
						stockdata.setOpen(Float.parseFloat(val));
						break;
					case "High":
						stockdata.setHigh(Float.parseFloat(val));
						break;
					case "Low":
						stockdata.setLow(Float.parseFloat(val));
						break;
					case "Close":
						stockdata.setClose(Float.parseFloat(val));
						break;
					case "Currency":
						stockdata.setCurrency(val);
						break;
					}
				}

				if (!Util.isValid(stockdata.getCurrency()))
					stockdata.setCurrency("INR");

				return stockdata;
			}).toList();
		} catch (FileNotFoundException e) {
			logger.debug(e.getMessage());
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}

		return data;
	}

	/**
	 * Return type of provider
	 */
	@Override
	public List<String> getType() {
		return type;
	}
}
