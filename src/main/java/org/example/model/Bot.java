package org.example.model;

import lombok.Getter;
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
    @Getter
    private InlineKeyboardMarkup currenciesKeyboard1;
    @Getter
    private InlineKeyboardMarkup currenciesKeyboard2;
    @Getter
    private InlineKeyboardMarkup currenciesKeyboard3;
    private ReplyKeyboardMarkup replyKeyboardMarkup;
    private final String BOT_USERNAME = "w0nder_waffle_bot";

    public Bot(String token, ExchangeRateGetter exchangeRateGetter) {
        this.token = token;
        this.exchangeRateGetter = exchangeRateGetter;
        createKeyboards();
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
                if (Flag.flags.containsKey(text)) {
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
                sendMenu(userId, "<b>Выберите валюту</b>", currenciesKeyboard1);
                break;
            case "/info":
                sendMessage(userId, getInformation());
                break;
            case "/start":
                sendMenu(userId, "Welcome", replyKeyboardMarkup);
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
                newKb.setReplyMarkup(currenciesKeyboard2);
            }
            case "Next2" -> {
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup(currenciesKeyboard3);
            }
            case "Back2" -> {
                System.out.println("BACK в очереди");
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup(currenciesKeyboard1);
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

        currenciesKeyboard1 = createKeyboard(4, 4, charCodeList1, 1, false, true);

        currenciesKeyboard2 = createKeyboard(4, 4, charCodeList2, 2, false, false);

        currenciesKeyboard3 = createKeyboard(4, 4, charCodeList3, 3, true, false);

    }

    /**
     * Метод создания телеграмм-клавиатуры
     *
     * @param rowSize           количество кнопок в строке
     * @param rowsQuantity      количество строк
     * @param charCodeList      список с кодами валют
     * @param isTheLustKeyboard последняя ли это клавиатура (от этого зависит наличие кнопки Next или Back)
     * @return клавиатуру с кнопками
     */
    private InlineKeyboardMarkup createKeyboard(int rowSize, int rowsQuantity, List<String> charCodeList, int keyBoardNumb, boolean isTheLustKeyboard, boolean isTheFirstKeyboard) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        //-1 - резерв для кнопки (Next/Back) в последней строке, если -2 - то для обоих кнопок (если клавиатура посередине других по логике)
        int valuteButtonsQuantity = isTheFirstKeyboard && isTheLustKeyboard ? rowsQuantity * rowSize - 2 : rowsQuantity * rowSize - 1;
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
                    rowButtons.add(createCurrencyButton(charCodeList.get(k)));
                }
                //когда элементы в списке заканчиваются - добавляем кнопки Next/Back
                else {
                    if (!isTheFirstKeyboard && !isTheLustKeyboard) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                        rowButtons.add(createInlineButton("Next", keyBoardNumb));
                    } else if (isTheLustKeyboard) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                    } else {
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
     * Метод создания кнопки валюты с флагом
     *
     * @param charCode код валюты
     * @return
     */
    private InlineKeyboardButton createCurrencyButton(String charCode) {
        return InlineKeyboardButton.builder()
                .text(charCode + Flag.flags.get(charCode))
                .callbackData(charCode)
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
                "/choose_currency - выбрать конкретную валюту";
    }
}
