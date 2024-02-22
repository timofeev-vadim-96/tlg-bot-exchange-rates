package org.example.services.exchangeRates;

import org.example.model.cbr.Valute;

import java.util.List;

public interface ExchangeRateGetter {
    public String getBasicExchangeRates();
    public String getAllExchangeRates();
    public String getSpecificExchangeRate(String charCode);
    public String convert(double rubles, String charCode);
    public List<Valute> getValutes();
}
