package org.example.services.exchangeRates;

import org.example.model.cbr.CbrResponse;
import org.example.model.cbr.Valute;
import org.example.util.Flag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CbrConnectorTest {
    private static CbrConnector connector;

    @BeforeAll
    static void setUp(){
        connector = new CbrConnector();
    }

    @Test
    void getExchangeRates() {
        CbrResponse response = connector.getExchangeRates(LocalDate.now());

        assertNotNull(response);
    }

    @Test
    void getBasicExchangeRates() {
        List<String> basicValutes = List.of("USD", "EUR", "GBP", "CNY", "JPY");

        String response = connector.getBasicExchangeRates();
        boolean statement = true;
        for (String str: basicValutes){
            String flag = Flag.flags.get(str);
            if (!response.contains(flag)) statement = false;
        }

        assertTrue(statement);
    }

    @Test
    void getAllExchangeRates() {
        String answer = connector.getAllExchangeRates();

        assertTrue(answer.length() > 1000);
    }


    @Test
    void getSpecificExchangeRate() {
        String specific = connector.getSpecificExchangeRate("CNY");

        assertTrue(specific.contains(Flag.flags.get("CNY")));
    }


    @Test
    void convertWhenCurrencyIsExists() {
        String converted = connector.convert(1000.00, "EUR");

        assertTrue(converted.contains(Flag.flags.get("EUR")));
    }

    @Test
    void convertWhenCurrencyIsNotExists() {
        String converted = connector.convert(1000.00, "FOO");

        assertTrue(converted.contains("не найдена"));
    }

    @Test
    void getDynamics() {
        String dynamics = connector.getDynamics("месяц", (short) 1, "USD");

        assertTrue(dynamics.contains("Динамика курса"));
        assertTrue(dynamics.contains(Flag.flags.get("USD")));
    }

    @Test
    void getValutes() {
        List<Valute> currencies = connector.getValutes();

        assertNotNull(currencies);
    }
}