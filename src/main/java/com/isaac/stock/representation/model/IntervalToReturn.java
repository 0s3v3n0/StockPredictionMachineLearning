package com.isaac.stock.representation.model;

public enum IntervalToReturn {

    FIVE_MIN("5m"),
    FIFTEEN_MIN("15m"),
    THIRTY_MIN("30m"),
    ONE_HOUR("1h"),
    ONE_DAY("1d"),
    ONE_WEEK("1wk"),
    ONE_MONTH("1mo"),
    THREE_MONTHS("3mo");


    public String value;

    IntervalToReturn(String s) {
        value = s;
    }
}

