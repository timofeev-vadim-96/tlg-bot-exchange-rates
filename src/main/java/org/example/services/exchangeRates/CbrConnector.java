package org.example.services.exchangeRates;

import org.example.model.cbr.CbrResponse;
import org.example.model.cbr.Valute;
import org.example.util.Flag;
import org.example.util.XmlConverterUtil;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
                stringBuilder.append(formatExchangeRate(valute) + "\n");
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
                stringBuilder.append(formatExchangeRate(valute) + "\n");
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
                .findFirst().map(this::formatExchangeRate)
                .orElse(String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode));
    }

    /**
     * Метод конвертации определенного количества рублей в валюту
     * @param rubles
     * @param charCode
     * @return
     */
    @Override
    public String convert(double rubles, String charCode) {
        CbrResponse response = getExchangeRates();
        Valute valute = response.getValutes().stream().filter(val -> val.getCharCode().equals(charCode))
                .findFirst().orElse(null);
        if (valute == null){
            return String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode);
        }
        double valuteAmount = rubles * Double.parseDouble(valute.getVUnitRate());
        return formatConvertResult(rubles, valuteAmount, charCode);
    }

    @Override
    public List<Valute> getValutes() {
        return getExchangeRates().getValutes();
    }

    /**
     * Метод форматирования конвертации в валюту
     * @param rubles рубли
     * @param valuteAmount сумма в валюте
     * @param charCode код валюты
     */
    private String formatConvertResult(double rubles, double valuteAmount, String charCode) {
        StringBuilder res = new StringBuilder(String.format("%f Российский рубль = %f %s",
                rubles, valuteAmount, charCode));
        if (Flag.flags.containsKey(charCode)) {
            res.append(Flag.flags.get(charCode));
        }
        return res.toString();
    }

    /**
     * Метод форматирования вывода курса валют
     *
     * @param valute объект валюты
     */
    private String formatExchangeRate(Valute valute) {
        StringBuilder res = new StringBuilder(String.format("%s Российский рубль = %d %s",
                valute.getValue(), valute.getNominal(), valute.getName()));
        if (Flag.flags.containsKey(valute.getCharCode())) {
            res.append(Flag.flags.get(valute.getCharCode()));
        }
        return res.toString();
    }
}
