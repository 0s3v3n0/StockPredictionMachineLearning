package com.isaac.stock.representation.model;

public class Meta{
	private String exchangeTimezoneName;
	private String symbol;
	private String instrumentType;
	private int firstTradeDate;
	private String timezone;
	private int scale;
	private String range;
	private int regularMarketTime;
	private String dataGranularity;
	private Object regularMarketPrice;
	private Object previousClose;
	private int gmtoffset;
	private Object chartPreviousClose;
	private int priceHint;
	private String currency;
	private String exchangeName;

	public String getExchangeTimezoneName(){
		return exchangeTimezoneName;
	}

	public String getSymbol(){
		return symbol;
	}

	public String getInstrumentType(){
		return instrumentType;
	}

	public int getFirstTradeDate(){
		return firstTradeDate;
	}

	public String getTimezone(){
		return timezone;
	}

	public int getScale(){
		return scale;
	}

	public String getRange(){
		return range;
	}

	public int getRegularMarketTime(){
		return regularMarketTime;
	}

	public String getDataGranularity(){
		return dataGranularity;
	}

	public Object getRegularMarketPrice(){
		return regularMarketPrice;
	}

	public Object getPreviousClose(){
		return previousClose;
	}

	public int getGmtoffset(){
		return gmtoffset;
	}

	public Object getChartPreviousClose(){
		return chartPreviousClose;
	}

	public int getPriceHint(){
		return priceHint;
	}

	public String getCurrency(){
		return currency;
	}

	public String getExchangeName(){
		return exchangeName;
	}
}
