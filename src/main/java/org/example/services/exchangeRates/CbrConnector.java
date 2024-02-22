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
     *
     * @param charCode символьный код искомой валюты
     */
    public String getSpecificExchangeRate(String charCode) {
        CbrResponse response = getExchangeRates();
        return response.getValutes().stream().filter(valute -> valute.getCharCode().equals(charCode))
                .findFirst().map(this::formatExchangeRate)
                .orElse(String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode));
    }

    /**
     * Метод конвертации определенного количества рублей в валюту
     *
     * @param rubles
     * @param charCode
     * @return
     */
    @Override
    public String convert(double rubles, String charCode) {
        CbrResponse response = getExchangeRates();
        Valute valute = response.getValutes().stream().filter(val -> val.getCharCode().equals(charCode))
                .findFirst().orElse(null);
        if (valute == null) {
            return String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode);
        }
        double valuteAmount = rubles / +Double.parseDouble(valute.getVUnitRate().replace(",", "."));
        return formatConvertResult(rubles, valuteAmount, valute);
    }

    @Override
    public List<Valute> getValutes() {
        return getExchangeRates().getValutes();
    }

    /**
     * Метод форматирования конвертации в валюту
     *
     * @param rubles       рубли
     * @param valuteAmount сумма в валюте
     * @param valute       валюта
     */
    private String formatConvertResult(double rubles, double valuteAmount, Valute valute) {
        String correctRublesPhrase = getCorrectPhraseCase(rubles);
        StringBuilder res = new StringBuilder(String.format("%.2f %s = %.2f %s",
                rubles, correctRublesPhrase, valuteAmount, valute.getName()));
        if (Flag.flags.containsKey(valute.getCharCode())) {
            res.append(Flag.flags.get(valute.getCharCode()));
        }
        return res.toString();
    }

    /**
     * Метод форматирования вывода курса валют
     *
     * @param valute объект валюты
     */
    private String formatExchangeRate(Valute valute) {
        String correctRublesPhrase = getCorrectPhraseCase(Double.parseDouble(valute.getValue()
                .replace(",", ".")));

        StringBuilder res = new StringBuilder(String.format("%s %s = %d %s",
                valute.getValue(), correctRublesPhrase, valute.getNominal(), valute.getName()));
        if (Flag.flags.containsKey(valute.getCharCode())) {
            res.append(Flag.flags.get(valute.getCharCode()));
        }
        return res.toString();
    }

    /**
     * Метод определения корректного падежа для российского рубля
     *
     * @param rubles рубли
     * @return
     */
    private String getCorrectPhraseCase(double rubles) {
        String rublesStr = String.valueOf((int) rubles); //избавляемся от дробной части

        List<String> case1 = List.of("11", "12", "13", "14"); //ль
        List<String> case2 = List.of("1", "01"); //ль
        List<String> case3 = List.of("2", "3", "4", "02", "03", "04"); //ля

        for (String str : case1) {
            if (rublesStr.endsWith(str)) return "Российских рублей";
        }
        for (String str : case2) {
            if (rublesStr.endsWith(str)) return "Российский рубль";
        }
        for (String str : case3) {
            if (rublesStr.endsWith(str)) return "Российских рубля";
        }
        return "Российских рублей";
    }
}
