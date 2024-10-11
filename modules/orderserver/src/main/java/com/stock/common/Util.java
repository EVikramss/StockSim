package com.stock.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Util {

	/**
	 * This method helps in reading input from console
	 * @return
	 * @throws IOException
	 */
	public static String readConsole() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String data = reader.readLine();
		return data;
	}

	/**
	 * Check if a string is not null or blank
	 * 
	 * @param currency
	 * @return
	 */
	public static boolean isValid(String currency) {
		return currency != null && currency.trim().length() > 0;
	}
	
	public static String getShortNameForOrderType(String className) {
		
		int classNameIndex = className.lastIndexOf(".");
		if(classNameIndex != -1) {
			className = className.substring(classNameIndex + 1);
		}
		
		className = className.replace("BuyOrder", "");
		className = className.replace("SellOrder", "");
		className = className.replace("Buy", "");
		className = className.replace("Sell", "");
		className = className.replace("Order", "");
		
		return className;
	}
}
