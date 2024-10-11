package com.stock.models;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * This table contains distinct list of supported stocks
 */
@Entity
public class StockName {

	@Id
	private String name;
	
	@JsonManagedReference
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "stockName")
	private Set<StockDay> days;
	
	public StockName() {
	}
	
	public StockName(String stockName) {
		this.name = stockName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<StockDay> getDays() {
		return days;
	}

	public void setDays(Set<StockDay> days) {
		this.days = days;
	}
}
