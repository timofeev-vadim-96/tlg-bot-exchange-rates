package org.example.services.exchangeRates;

import org.example.model.cbr.Valute;

import java.util.List;

public interface ExchangeRateGetter {
    String getBasicExchangeRates();
    String getAllExchangeRates();
    String getSpecificExchangeRate(String charCode);
    String convert(double rubles, String charCode);
    List<Valute> getValutes();
    String getDynamics(String timeUnit, short unit, String charCode);
}
