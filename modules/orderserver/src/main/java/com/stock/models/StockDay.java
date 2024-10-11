package com.stock.models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * This table maintains the days on which data is available for a stock to show.
 */
@Entity
public class StockDay {

	@Id
	private Long id;

	private String stockDate;
	
	@ManyToOne
	@JsonBackReference
	private StockName stockName;

	public StockDay() {
	}

	public StockDay(String stockDate) {
		this.stockDate = stockDate;
	}

	public String getStockDate() {
		return stockDate;
	}

	public void setStockDate(String stockDate) {
		this.stockDate = stockDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return stockDate.hashCode();
	}

	public StockName getStockName() {
		return stockName;
	}

	public void setStockName(StockName stockName) {
		this.stockName = stockName;
	}
}
