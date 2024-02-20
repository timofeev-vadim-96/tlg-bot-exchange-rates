package org.example.services.exchangeRates;

public interface ExchangeRateGetter {
    public String getBasicExchangeRates();
    public String getAllExchangeRates();
    public String getSpecificExchangeRate(String charCode);
}
