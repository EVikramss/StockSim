package com.stock.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stock.models.StockData;

import io.micrometer.core.annotation.Timed;

@Service
public class PricingService {

	@Autowired
	private StockDataService dataService;

	/**
	 * Get stock data for a particular day.
	 * 
	 * @param stockName
	 * @param day
	 * @return
	 */
	public List<StockData> getStockData(String stockName, String day) {
		return dataService.getStockData(stockName, day);
	}

	/**
	 * Recursively search for time bracket - need to improve impl;
	 * 
	 * @param data
	 * @param hour
	 * @param min
	 * @param sec
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private Integer[] getGivenTimeOpenPoints(List<StockData> data, int hour, int min, int sec, int startIndex,
			int endIndex) {

		int midIndex = (endIndex + startIndex) / 2;
		String midTime = data.get(midIndex).getTime();
		Integer[] output = new Integer[] { -1, -1 };

		Integer midHour = Integer.parseInt(midTime.split(":")[0]);
		Integer midMin = Integer.parseInt(midTime.split(":")[1]);

		if ((midHour + ":" + midMin).equals(hour + ":" + min)) {
			output[0] = midIndex;
		} else {
			if (midHour < hour || (midHour == hour && midMin < min)) {
				if (endIndex == startIndex) {
					output[0] = midIndex;
					output[1] = midIndex + 1 <= data.size() - 1 ? midIndex + 1 : midIndex;
				} else if (midIndex != data.size() - 1) {
					return getGivenTimeOpenPoints(data, hour, min, sec, midIndex + 1, endIndex);
				} else {
					output[0] = midIndex;
				}
			} else {
				if (endIndex == startIndex) {
					output[0] = midIndex - 1 >= 0 ? midIndex - 1 : midIndex;
					output[1] = midIndex;
				} else if (midIndex != 0) {
					return getGivenTimeOpenPoints(data, hour, min, sec, 0, midIndex - 1);
				} else {
					output[0] = midIndex;
				}
			}
		}

		return output;
	}

	/**
	 * Get price of stock, interpolate if needed.
	 * 
	 * @param data
	 * @param strHour
	 * @param strMinute
	 * @param strMinuteOffset
	 * @return
	 */
	@Timed(value = "PricingService.getPrice")
	public String[] getPrice(List<StockData> data, String strHour, String strMinute, String strMinuteOffset) {

		String[] output = new String[] { null, null };

		// get final hour by adding minute offset
		int hour = Integer.parseInt(strHour);
		int min = Integer.parseInt(strMinute);
		int minOffset = Integer.parseInt(strMinuteOffset);
		int hourOffSet = minOffset / 60;
		minOffset = minOffset - (hourOffSet * 60);

		hour += hourOffSet;
		min += minOffset;

		if (min > 59) {
			int newHrOffset = min / 60;
			min = min - (newHrOffset * 60);
			hour += newHrOffset;
		}
		Random random = new Random(minOffset);
		int sec = (int) (random.nextDouble() * 60.0);

		// get data for given time
		Integer[] vals = getGivenTimeOpenPoints(data, hour, min, sec, 0, data.size() - 1);
		if (vals[1] == -1 || vals[0] == vals[1]) {
			StockData dataObj = data.get(vals[0]);
			output[0] = dataObj.getTime();
			output[1] = dataObj.getOpen() + "";
		} else {
			StockData dataObj1 = data.get(vals[0]);
			StockData dataObj2 = data.get(vals[1]);

			String time1 = dataObj1.getTime();
			String time2 = dataObj2.getTime();

			float time1Val = ((Float.parseFloat(time1.split(":")[0]) * 60.0f) + Float.parseFloat(time1.split(":")[1]))
					* 60.0f;
			float time2Val = ((Float.parseFloat(time2.split(":")[0]) * 60.0f) + Float.parseFloat(time2.split(":")[1]))
					* 60.0f;
			float timeVal = (((hour * 60.0f) + min) * 60.0f) + sec;

			float val1 = dataObj1.getOpen();
			float val2 = dataObj2.getOpen();

			float val = (((val2 - val1) / (time2Val - time1Val)) * (timeVal - time1Val)) + val1;
			val += random.nextDouble() * val * 0.05;
			
			output[0] = (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min);
			output[1] = val + "";
		}

		return output;
	}

	/**
	 * Get current price of stock, interpolate if needed
	 * 
	 * @param data
	 * @return
	 */
	@Timed(value = "PricingService.getCurrentPrice")
	public String[] getCurrentPrice(List<StockData> data) {

		String[] output = new String[] { null };

		// get current jvm time
		LocalTime time = LocalTime.now();
		int hour = time.getHour();
		int min = time.getMinute();
		int sec = time.getSecond();

		Random random = new Random(sec);

		Integer[] vals = getGivenTimeOpenPoints(data, hour, min, sec, 0, data.size() - 1);
		if (vals[1] == -1 || vals[0] == vals[1]) {
			StockData dataObj = data.get(vals[0]);
			output[0] = dataObj.getTime();
			output[1] = dataObj.getOpen() + "";
		} else {
			StockData dataObj1 = data.get(vals[0]);
			StockData dataObj2 = data.get(vals[1]);

			String time1 = dataObj1.getTime();
			String time2 = dataObj2.getTime();

			float time1Val = ((Float.parseFloat(time1.split(":")[0]) * 60.0f) + Float.parseFloat(time1.split(":")[1]))
					* 60.0f;
			float time2Val = ((Float.parseFloat(time2.split(":")[0]) * 60.0f) + Float.parseFloat(time2.split(":")[1]))
					* 60.0f;
			float timeVal = (((hour * 60.0f) + min) * 60.0f) + sec;

			float val1 = dataObj1.getOpen();
			float val2 = dataObj2.getOpen();

			float val = (((val2 - val1) / (time2Val - time1Val)) * (timeVal - time1Val)) + val1;
			val += random.nextDouble() * val * 0.05;
			
			output[0] = (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min);
			output[1] = val + "";
		}

		return output;
	}
}
