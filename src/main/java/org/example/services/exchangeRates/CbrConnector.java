package org.example.services.exchangeRates;

import org.example.model.cbr.CbrResponse;
import org.example.model.cbr.Valute;
import org.example.util.Flag;
import org.example.util.XmlConverterUtil;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class CbrConnector implements ExchangeRateGetter {

    /**
     * Метод по обращению к API Центробанка РФ и получения курсов валют в виде Объекта
     *
     * @return CbrResponse объект, содержащий список валют
     */
    public CbrResponse getExchangeRates() {
        try {
            URL url = new URL("https://cbr.ru/scripts/XML_daily.asp");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            InputStreamReader inputStreamReader = new InputStreamReader(con.getInputStream());
            CbrResponse response = XmlConverterUtil.unmarshallStream(inputStreamReader, CbrResponse.class);
            inputStreamReader.close();

            return response;
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Курс пяти основных валют: (Доллар США, Евро, Юань, Фунт стерлинга и Японская иена)
     */
    public String getBasicExchangeRates() {
        CbrResponse response = getExchangeRates();
        StringBuilder stringBuilder = new StringBuilder();
        for (Valute valute : response.getValutes()) {
            String charCode = valute.getCharCode();
            if (charCode.equals("USD") || charCode.equals("EUR") ||
                    charCode.equals("CNY") || charCode.equals("GBP") || charCode.equals("JPY")) {
                stringBuilder.append(formatExchangeRate(valute) + Flag.flags.get(valute.getCharCode()) + "\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Курс всех возможных валют
     */
    public String getAllExchangeRates() {
        CbrResponse response = getExchangeRates();
        StringBuilder stringBuilder = new StringBuilder();
        for (Valute valute : response.getValutes()) {
            if (Flag.flags.containsKey(valute.getCharCode())) {
                stringBuilder.append(formatExchangeRate(valute) + Flag.flags.get(valute.getCharCode()) + "\n");
            } else {
                stringBuilder.append(formatExchangeRate(valute) + "\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Метод для получения курса конкретной валюты
     * @param charCode символьный код искомой валюты
     */
    public String getSpecificExchangeRate(String charCode){
        CbrResponse response = getExchangeRates();
        return response.getValutes().stream().filter(valute -> valute.getCharCode().equals(charCode))
                .findFirst().map(valute -> formatExchangeRate(valute) + Flag.flags.get(valute.getCharCode()))
                .orElse(String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode));
    }

    /**
     * Метод форматирования вывода курса валют
     *
     * @param valute объект валюты
     */
    private String formatExchangeRate(Valute valute) {
        return String.format("%s Российский рубль = %d %s",
                valute.getValue(), valute.getNominal(), valute.getName());
    }

}
