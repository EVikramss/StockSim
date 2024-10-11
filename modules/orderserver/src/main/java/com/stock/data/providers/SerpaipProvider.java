package com.stock.data.providers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stock.LoadData;
import com.stock.models.StockData;

/**
 * This class is used to fetch stock data from serpaip - in progress
 */
@Component
public class SerpaipProvider implements DataProvider {

	Logger logger = LogManager.getLogger(SerpaipProvider.class);
	

	private List<String> type = Arrays.asList("CURRENT", "JSON");
	private List<StockData> stockDataList = null;

	/**
	 * Get data by searching through listed directory
	 */
	@Override
	public List<StockData> getData(Long startingKey) {

		stockDataList = new ArrayList<StockData>();
		searchAndProcessDir(new File(LoadData.serpaipFileDir));

		// write to stack, so that list on heap gets GC'd
		List<StockData> newStockDataList = LongStream.range(startingKey, startingKey + stockDataList.size())
				.sequential().mapToObj(i -> {
					StockData data = stockDataList.get((int) (i - startingKey));
					data.setId(i);
					return data;
				}).toList();
		stockDataList = null;

		return newStockDataList;
	}

	/**
	 * This method recursively checks a directory and processes files
	 * 
	 * @param file
	 */
	private void searchAndProcessDir(File file) {
		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				searchAndProcessDir(subFile);
			}
		} else {
			processFile(file);
		}
	}

	/**
	 * This method reads a file and process stock data from it.
	 * 
	 * @param file
	 */
	private void processFile(File file) {
		try {
			String dataFile = Files.readString(Paths.get(file.toURI()));

			String searchStr = "\"search_parameters\":";
			int startIndex = dataFile.indexOf(searchStr);
			int endIndex = dataFile.indexOf("}", startIndex) + 1;
			String searchData = "{" + dataFile.substring(startIndex, endIndex) + "}";

			JsonObject jsonObject = new JsonParser().parse(searchData).getAsJsonObject();
			String instrumentName = jsonObject.getAsJsonObject("search_parameters").get("q").getAsString();

			searchStr = "\"graph\":\r\n[";
			startIndex = dataFile.indexOf(searchStr);
			endIndex = dataFile.indexOf("]", startIndex) + 1;
			searchData = "{" + dataFile.substring(startIndex, endIndex) + "}";

			jsonObject = new JsonParser().parse(searchData).getAsJsonObject();
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMM dd yyyy, hh:mm a z");
			JsonArray array = jsonObject.getAsJsonArray("graph");
			for (int i = 0; i < array.size(); i++) {
				JsonObject obj = (JsonObject) array.get(i);
				float price = obj.get("price").getAsFloat();
				String currency = obj.get("currency").getAsString();
				String date = obj.get("date").getAsString();
				int volume = obj.get("volume").getAsInt();

				int dateReplIndexstart = date.indexOf("AM");
				if (dateReplIndexstart == -1) {
					dateReplIndexstart = date.indexOf("PM");
				}
				int lengthFromEnd = date.length() - 6;

				date = date.substring(0, dateReplIndexstart + 3) + date.substring(lengthFromEnd);

				ZonedDateTime zdt = ZonedDateTime.parse(date, inputFormatter);
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
				String formattedDate = zdt.format(dateFormatter);
				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
				String formattedTime = zdt.format(timeFormatter);

				StockData stockdata = new StockData();
				stockdata.setInstrument(instrumentName);
				stockdata.setDate(formattedDate);
				stockdata.setTime(formattedTime);
				stockdata.setOpen(price);
				stockdata.setCurrency(currency);
				stockdata.setVolume(volume);
				stockDataList.add(stockdata);
			}
		} catch (IOException e) {
			logger.debug("Error processing file " + file.getAbsolutePath() + " . Error " + e.getMessage());
		}
	}

	/**
	 * Return type of provider
	 */
	@Override
	public List<String> getType() {
		return type;
	}

}
