package org.example.services.exchangeRates;

import org.example.model.cbr.CbrResponse;
import org.example.model.cbr.Valute;
import org.example.util.Flag;
import org.example.util.XmlConverterUtil;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CbrConnector implements ExchangeRateGetter {
    private final String DEFAULT_URL = "https://cbr.ru/scripts/XML_daily.asp";
    private List<String> basicValutes = List.of("USD", "EUR", "GBP", "CNY", "JPY");

    /**
     * Метод по обращению к API Центробанка РФ и получения курсов валют в виде Объекта
     *
     * @return CbrResponse объект, содержащий список валют
     */
    public CbrResponse getExchangeRates(LocalDate date) {
        try {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            URL url = new URL(DEFAULT_URL + "?date_req=" + formattedDate);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            InputStreamReader inputStreamReader = new InputStreamReader(con.getInputStream());
            CbrResponse response = XmlConverterUtil.unmarshall(inputStreamReader, CbrResponse.class);
            inputStreamReader.close();

            return response;
        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Курс пяти основных валют: (Доллар США, Евро, Юань, Фунт стерлинга и Японская иена)
     */
    public String getBasicExchangeRates(LocalDate date) {
        CbrResponse response = getExchangeRates(date);
        StringBuilder stringBuilder = new StringBuilder();
        for (Valute valute : response.getValutes()) {
            String charCode = valute.getCharCode();
            if (basicValutes.contains(charCode)) {
                stringBuilder.append(formatExchangeRate(valute));
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String getBasicExchangeRates() {
        LocalDate now = LocalDate.now();
        return getBasicExchangeRates(now);
    }

    /**
     * Курс всех возможных валют
     */
    public String getAllExchangeRates(LocalDate date) {
        CbrResponse response = getExchangeRates(date);
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

    @Override
    public String getAllExchangeRates() {
        LocalDate now = LocalDate.now();
        return getAllExchangeRates(now);
    }

    /**
     * Метод для получения курса конкретной валюты
     *
     * @param charCode символьный код искомой валюты
     */
    public String getSpecificExchangeRate(String charCode, LocalDate date) {
        CbrResponse response = getExchangeRates(date);
        return response.getValutes().stream().filter(valute -> valute.getCharCode().equals(charCode))
                .findFirst().map(this::formatExchangeRate)
                .orElse(String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode));
    }

    @Override
    public String getSpecificExchangeRate(String charCode) {
        LocalDate date = LocalDate.now();
        return getSpecificExchangeRate(charCode, date);
    }

    /**
     * Метод конвертации определенного количества рублей в валюту
     *
     * @param rubles
     * @param charCode
     * @return
     */
    public String convert(double rubles, String charCode, LocalDate date) {
        CbrResponse response = getExchangeRates(date);
        Valute valute = response.getValutes().stream().filter(val -> val.getCharCode().equals(charCode))
                .findFirst().orElse(null);
        if (valute == null) {
            return String.format("Валюта с таким символьным кодом \"%s\" не найдена.", charCode);
        }
        double valuteAmount = rubles / +Double.parseDouble(valute.getVUnitRate().replace(",", "."));
        return formatConvertResult(rubles, valuteAmount, valute);
    }

    @Override
    public String convert(double rubles, String charCode) {
        LocalDate now = LocalDate.now();
        return convert(rubles, charCode, now);
    }

    /**
     * Метод определения динамики основных валют
     *
     * @param timeUnit - единица времени: неделя/месяц/год
     * @param unit     - количество недель/месяцев/лет
     * @return динамику основных валют
     */
    public String getDynamics(String timeUnit, short unit, String charCode) {
        String downgradeSymbol = "\uD83D\uDCC9";
        String upgradeSymbol = "\uD83D\uDCC8";

        LocalDate pastDate = getDateFromThePast(timeUnit, unit);
        String formattedPastDate = pastDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        CbrResponse responsePresent = getExchangeRates(LocalDate.now());
        CbrResponse responsePast = getExchangeRates(pastDate);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("<b>Динамика курса %s\n" +
                "Период: %d %s, с %s по н.в.</b>\n", charCode, unit, timeUnit, formattedPastDate));

        Valute valutePast = getValute(charCode, responsePast.getValutes());
        Valute valutePresent = getValute(charCode, responsePresent.getValutes());
        double pastValue = Double.parseDouble(valutePast.getValue().replace(",", "."));
        double presentValue = Double.parseDouble(valutePresent.getValue().replace(",", "."));
        String symbol = pastValue <= presentValue ? upgradeSymbol : downgradeSymbol;
        double dynamic = Math.abs(100.00 - presentValue * 100 / pastValue);
        if (Flag.flags.containsKey(charCode)) {
            stringBuilder.append(Flag.flags.get(charCode));
        }
        stringBuilder.append(String.format("%s %s на %.2f%%, с %.2f до %.2f %s\n",
                valutePresent.getName(), symbol, dynamic,
                pastValue, presentValue, getCorrectPhraseCase(presentValue)));
        return stringBuilder.toString();
    }

    /**
     * Метод сравнивает пришедшие параметры, преобразует timeunit в корректный формат и возвращает дату в прошлом
     *
     * @param timeUnit единица времени
     * @param unit     числовое значение
     * @return корректную дату в прошлом
     */
    private static LocalDate getDateFromThePast(String timeUnit, short unit) {
        LocalDate now = LocalDate.now();
        LocalDate pastDate;
        if (timeUnit.contains("нед")) {
            pastDate = now.minusWeeks(unit);
        } else if (timeUnit.contains("мес")) {
            pastDate = now.minusMonths(unit);
        } else if (timeUnit.contains("лет") || timeUnit.contains("год")) {
            pastDate = now.minusYears(unit);
        } else throw new IllegalArgumentException("invalid value of an argument timeunit : " + timeUnit);
        return pastDate;
    }

    /**
     * Метод возвращения объекта Валюты по символьному коду
     *
     * @param charCode символьный код
     * @param valutes  список валют
     * @return Valute или null, если валюты с таким charCode в списке нет
     */
    private Valute getValute(String charCode, List<Valute> valutes) {
        return valutes.stream().filter(val -> val.getCharCode().equals(charCode))
                .findFirst().orElse(null);
    }


    public List<Valute> getValutes(LocalDate date) {
        return getExchangeRates(date).getValutes();
    }

    @Override
    public List<Valute> getValutes() {
        LocalDate now = LocalDate.now();
        return getValutes(now);
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

        double value = Double.parseDouble(valute.getValue().replace(",", "."));

        StringBuilder res = new StringBuilder(String.format("%.2f %s = %d %s",
                value, correctRublesPhrase, valute.getNominal(), valute.getName()));
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
