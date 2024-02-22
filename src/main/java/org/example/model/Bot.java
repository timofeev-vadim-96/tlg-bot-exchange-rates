package org.example.model;

import lombok.Getter;
import org.example.model.cbr.Valute;
import org.example.services.exchangeRates.ExchangeRateGetter;
import org.example.util.Flag;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private ExchangeRateGetter exchangeRateGetter;
    private final String token;


    //клавиатура для команды /start
    private ReplyKeyboardMarkup replyKeyboardMarkup;
    //клавиатура для команды /convert
    private InlineKeyboardMarkup convertKeyboard;
    private final String BOT_USERNAME = "w0nder_waffle_bot";
    private final String DOUBLE_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
    //для идентификации callbackData от кнопок - соответствия их клавиатуре
    private static int keyboardsTotalCounter = 1;
    //клавиатуры для котировок валют (кол-во рассчитывается автоматически. Зависит от размер n x m и от кол-ва валют)
    List<InlineKeyboardMarkup> exchangeKeyboards;

    public Bot(String token, ExchangeRateGetter exchangeRateGetter) {
        this.token = token;
        this.exchangeRateGetter = exchangeRateGetter;

        exchangeKeyboards = createKeyboards(4,4,exchangeRateGetter.getValutes());

        createReplyKeyboard();
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        //обработка CallbackQuery от нажатия кнопок
        if (update.hasCallbackQuery()) {
            String text = update.getCallbackQuery().getData();
            try {
                if (Flag.flags.containsKey(text)) { //пустые приходят от клавиатур котировок валют (базовых)
                    sendMessage(update.getCallbackQuery().getFrom().getId(), exchangeRateGetter.getSpecificExchangeRate(text));
                } else buttonTap(update.getCallbackQuery());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        } else if (update.hasMessage()) {
            Long userId = update.getMessage().getFrom().getId();
            Message message = update.getMessage();
            if (message.isCommand()) {
                commandsHandler(userId, message);
            } else {
                handleMessage(userId, message);
            }
        }
    }

    /**
     * Метод обработки сообщений, содержащих ключевые фразы
     *
     * @param userId  id пользователя
     * @param message сообщение
     */
    public void handleMessage(long userId, Message message) {
        String text = message.getText();
        if (text.equals("Информация о боте")) {
            sendMessage(userId, getInformation());
        }
    }

    /**
     * Метод отправки простого сообщения
     *
     * @param userId  id пользователя
     * @param message сообщение
     */
    public void sendMessage(long userId, String message) {
        SendMessage sm = SendMessage.builder()
                .chatId(String.valueOf(userId)) //id пользователя
                .text(message) //текст сообщения
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод - обработчик команд
     *
     * @param userId  id пользователя
     * @param message сообщение
     */
    private void commandsHandler(Long userId, Message message) {
        String command = message.getText();
        switch (command) {
            case "/exchange_rates":
                sendMessage(userId, exchangeRateGetter.getBasicExchangeRates());
                break;
            case "/exchange_rates_all":
                sendMessage(userId, exchangeRateGetter.getAllExchangeRates());
                break;
            case "/choose_currency":
                sendMenu(userId, "<b>Выберите валюту</b>", (exchangeKeyboards.get(0)));
                break;
            case "/convert":
                //TODO конвертация
                String[] convertArr = command.split(" ");
                if (convertArr.length != 2 || !convertArr[1].matches(DOUBLE_REGEX)) { //2 - команда + сумма рублей
                    sendMessage(userId, "Неверные параметры для конвертации.");
                }
                break;
            case "/info":
                sendMessage(userId, getInformation());
                break;
            case "/start":
                sendMenu(userId, "Welcome", replyKeyboardMarkup);
                break;
            case "/feedback":
                //TODO feedback
                break;
            case "/dynamics":
                //TODO Динамика ключевых валют за 1, 2, 3, 6, 12, 24, 36, 60 месяцев
                break;
            default:
                sendMessage(userId, "Unknown command!");
                break;
        }
    }

    /**
     * Метод отправки сообщения с клавиатурой
     *
     * @param id  id пользователя
     * @param txt текст
     * @param kb  клавиатура
     */
    public void sendMenu(Long id, String txt, ReplyKeyboard kb) {
        SendMessage sm = SendMessage.builder()
                .chatId(id.toString())
                .parseMode("HTML")
                .text(txt)
                .replyMarkup(kb)
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод логики при нажатии кнопок
     *
     * @param query обратный запрос при нажатии кнопки
     * @throws TelegramApiException
     */
    private void buttonTap(CallbackQuery query) throws TelegramApiException {
        Long id = query.getFrom().getId(); //id пользователя, нажавшего кнопку
        String queryId = query.getId(); //нужен для того, чтобы закрыть CallbackQuery
        String data = query.getData(); //идентифицирует кнопку, которая была нажата
        int msgId = query.getMessage().getMessageId(); //id сообщения

        //новый заголовок для меню
        EditMessageText newTxt = EditMessageText.builder()
                .chatId(id.toString())
                .parseMode("HTML")
                .messageId(msgId).text("").build();

        //новая клавиатура
        EditMessageReplyMarkup newKb = EditMessageReplyMarkup.builder()
                .chatId(id.toString()).messageId(msgId).build();

        switch (data) {
            case "Next1", "Back3" -> {
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup(exchangeKeyboards.get(1));
            }
            case "Next2" -> {
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup((exchangeKeyboards.get(2)));
            }
            case "Back2" -> {
                System.out.println("BACK в очереди");
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup((exchangeKeyboards.get(0)));
            }
        }

        //Всегда закрывать запросы от кнопок
        AnswerCallbackQuery close = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId).build();

        execute(close);
        execute(newTxt);
        execute(newKb);
    }

    /**
     * Метод инициализация клавиатур
     */
    private void createKeyboards() {
        List<String> charCodeList1 = List.of("AUD", "AZN", "GBP", "AMD", "BYN", "BGN", "BRL", "HUF", "VND", "HKD", "GEL", "DKK", "AED", "USD", "EUR");
        List<String> charCodeList2 = List.of("EGP", "INR", "IDR", "KZT", "CAD", "QAR", "KGS", "CNY", "MDL", "NZD", "NOK", "PLN", "RON", "XDR");
        List<String> charCodeList3 = List.of("TJS", "THB", "TRY", "TMT", "UZS", "UAH", "CZK", "SEK", "CHF", "RSD", "ZAR", "KRW", "JPY", "SGD");
    }

    /**
     * Метод инициализация клавиатур
     */
    private List<InlineKeyboardMarkup> createKeyboards(int rows, int columns, List<Valute> valutes) {
        List<String> charCodesAll = valutes.stream().map(Valute::getCharCode).toList();
        List<InlineKeyboardMarkup> keyboards = new ArrayList<>();
        int keyboardsQuantity = getKeyboardsQuantity(rows, columns, charCodesAll);
        int subListStartPosition = 0;
        int subListEndPosition = 0;
        for (int i = 0; i < keyboardsQuantity; i++) {
            //если клавиатура первая и не является единственной - кнопка Next нужна
            boolean needNextButton = i != keyboardsQuantity -1;
            //если клавиатура последняя и не единственная - кнопка Back нужна
            boolean needBackButton = i != 0;
            int valuteButtonsQuantity = getValuteButtonsQuantity(rows, columns, needNextButton, needBackButton);
            //todo
            System.out.println("количество кнопок на " + keyboardsTotalCounter + " клавиатуре - " + valuteButtonsQuantity + " Нужна ли " +
                    "стартовая кнопка : " + needNextButton + " или обратная кнопка " + needBackButton);
            subListEndPosition += valuteButtonsQuantity;
            //если это последняя клавиатуры, то границей будет - размер списка
            if (subListEndPosition >= charCodesAll.size()) subListEndPosition = charCodesAll.size();
            List<String> charCodesSublist = charCodesAll.subList(subListStartPosition, subListEndPosition);
            keyboards.add(createKeyboard(columns, rows, charCodesSublist, keyboardsTotalCounter++,
                    needNextButton, needBackButton));
            subListStartPosition += valuteButtonsQuantity;
        }
        return keyboards;
    }

    /**
     * Метод для расчета необходимого количества клавиатур относительно имеющихся Валют
     * @param rows строки
     * @param columns колонки
     * @param charCodesAll список имеющихся Валют
     * @return количество клавиатур
     */
    private static int getKeyboardsQuantity(int rows, int columns, List<String> charCodesAll) {
        int edgeKeyboardButtonMaxQuantity = rows * columns - 1; //1 место для кнопки Next ИЛИ Back
        int middleKeyboardButtonMaxQuantity = rows * columns - 2; //2 места для кнопок Next И Back
        int keyboardsQuantity;
        if (charCodesAll.size() <= rows * columns) {
            keyboardsQuantity = 1;
        }
        else if (charCodesAll.size() <= edgeKeyboardButtonMaxQuantity * 2) {
            keyboardsQuantity = 2;
        }
        else {
            keyboardsQuantity = 2; //учитываем прошлые проверки - уже точно более двух клавиатур
            if (((charCodesAll.size() - edgeKeyboardButtonMaxQuantity * 2) % middleKeyboardButtonMaxQuantity) == 0) {
                keyboardsQuantity += (charCodesAll.size() - edgeKeyboardButtonMaxQuantity * 2) / middleKeyboardButtonMaxQuantity;
            } else {
                keyboardsQuantity += (charCodesAll.size() - edgeKeyboardButtonMaxQuantity * 2) / middleKeyboardButtonMaxQuantity + 1; //+1 если какие-то кнопки не поместились
            }

        }
        return keyboardsQuantity;
    }

    /**
     * Метод создания телеграмм-клавиатуры
     *
     * @param rowSize           количество кнопок в строке
     * @param rowsQuantity      количество строк
     * @param charCodeList      список с кодами валют
     * @param needNextButton последняя ли это клавиатура (от этого зависит наличие кнопки Next или Back)
     * @return клавиатуру с кнопками
     */
    private InlineKeyboardMarkup createKeyboard(int rowSize, int rowsQuantity, List<String> charCodeList, int keyBoardNumb, boolean needNextButton, boolean needBackButton) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        //-1 - резерв для кнопки (Next/Back) в последней строке, если -2 - то для обоих кнопок (если клавиатура посередине других по логике)
        int valuteButtonsQuantity = getValuteButtonsQuantity(rowSize, rowsQuantity, needNextButton, needBackButton);
        if (valuteButtonsQuantity < charCodeList.size()) {
            throw new ArrayIndexOutOfBoundsException("размер переданного массива для создания телеграмм-клавиатуры " +
                    "больше, чем количество ячеек в ней");
        }

        List<List<InlineKeyboardButton>> entireKeyboardButtons = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < rowsQuantity; i++) {
            List<InlineKeyboardButton> rowButtons = new ArrayList<>();
            for (int j = 0; j < rowSize; j++) {

                if (k < charCodeList.size()) {
                    rowButtons.add(createCurrencyButton(charCodeList.get(k), ""));
                }
                //когда элементы в списке заканчиваются - добавляем кнопки Next/Back
                else {
                    if (needBackButton && needNextButton) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                        rowButtons.add(createInlineButton("Next", keyBoardNumb));
                    } else if (needBackButton) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                    } else if (needNextButton){
                        rowButtons.add(createInlineButton("Next", keyBoardNumb));
                    }
                    break;
                }
                k++;
            }
            entireKeyboardButtons.add(rowButtons);
        }
        keyboardMarkup.setKeyboard(entireKeyboardButtons);
        return keyboardMarkup;
    }

    /**
     * Метод для определения количества свободных кнопок на клавиатуре
     * @param rowSize кнопок в строке
     * @param rowsQuantity количество строк
     * @param needNextButton является ли клавиатура крайней
     * @param needBackButton является ли клавиатура первой
     * @return колич-во свободных кнопок с учетом места под кнопки перехода
     */
    private static int getValuteButtonsQuantity(int rowSize, int rowsQuantity, boolean needNextButton, boolean needBackButton) {
        //если это единственная клавиатура (кнопки Next и Back не нужны)
        if (needBackButton && needNextButton) return rowsQuantity * rowSize -2;
        //если это первая или последняя клавиатура - резерв под одну кнопку Next или Back
        else if (needBackButton || needNextButton) return rowsQuantity * rowSize - 1;
        //если это промежуточная клавиатура - нужны обе кнопки Next и Back
        else return rowsQuantity * rowSize;
    }

    /**
     * Метод создания кнопки валюты с флагом
     *
     * @param charCode    код валюты
     * @param affiliation принадлежность кнопки (для отслеживания callbackData с разных клавиатур от разных команд)
     * @return
     */
    private InlineKeyboardButton createCurrencyButton(String charCode, String affiliation) {
        return InlineKeyboardButton.builder()
                .text(charCode + Flag.flags.get(charCode))
                .callbackData(charCode + affiliation)
                .build();
    }

    /**
     * Метод для создания кнопки Next/Back
     *
     * @param text текст кнопки
     */
    private InlineKeyboardButton createInlineButton(String text, int keyBoardNumb) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(text + keyBoardNumb)
                .build();
    }

    /**
     * Клавиатура для стартового сценария
     */
    private void createReplyKeyboard() {
        replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подогнать под размер экрана
        replyKeyboardMarkup.setOneTimeKeyboard(true); //скрыть после использования (одноразовая)

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton("Информация о боте"))));

        replyKeyboardMarkup.setKeyboard(keyboardRows);
    }

    /**
     * Информация о боте
     */
    private String getInformation() {
        return "Данный телеграмм-бот предоставляет актуальные котировки валют,\n" +
                "основываясь на данных от Центробанка РФ\uD83C\uDDF7\uD83C\uDDFA \n" +
                "Функционал бота: \n" +
                "/start - начало работы\n" +
                "/info - о боте\n" +
                "/exchange_rates - котировки топ 5 валют Мира\n" +
                "/exchange_rates_all - котировки всех валют Центробанка РФ\n" +
                "/choose_currency - выбрать конкретную валюту\n" +
                "/convert 1000 - конвертация в валюту по текущему курсу";
    }
}
