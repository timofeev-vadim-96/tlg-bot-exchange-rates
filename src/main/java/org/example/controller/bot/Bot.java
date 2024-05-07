package org.example.controller.bot;

import lombok.Getter;
import lombok.SneakyThrows;
import org.example.controller.subscription.ScheduledNotifier;
import org.example.controller.users.UsersController;
import org.example.model.User.CustomUser;
import org.example.model.User.Subscription;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    @Getter
    private ExchangeRateGetter exchangeRateGetter;
    private UsersController controller;
    private ScheduledNotifier scheduledNotifier;
    private final String token;
    //клавиатура для команды /start
    private ReplyKeyboardMarkup replyKeyboardMarkup;
    //клавиатура для команды /convert
    private List<InlineKeyboardMarkup> convertKeyboards;
    private final String BOT_USERNAME = "w0nder_waffle_bot";
    private final String DOUBLE_REGEX = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
    //идентификатор(счетчик) клавиатур
    private static int keyboardsTotalCounter = 1;
    //клавиатуры для котировок валют (кол-во рассчитывается автоматически. Зависит от размер n x m и от кол-ва валют)
    private List<InlineKeyboardMarkup> exchangeKeyboards;
    private List<InlineKeyboardMarkup> subscribeKeyboards;
    private Map<Long, List<InlineKeyboardMarkup>> personalSubscriptionsKeyboardsLists;
    //клавиатура для периодов динамики курсов
    private List<InlineKeyboardMarkup> dynamicPeriodsKeyboards;
    //клавиатура для выбора валюты для вывода динамики курсов
    private List<InlineKeyboardMarkup> dynamicValuteChoiceKeyboards;

    public Bot(String token, ExchangeRateGetter exchangeRateGetter, UsersController controller) {
        this.token = token;
        this.exchangeRateGetter = exchangeRateGetter;
        this.controller = controller;

        //запуск рассылки подписок (под капотом новые потоки)
        Map<Integer, Integer> tasksTimes = Map.of(10, 0, 19, 0);
        scheduledNotifier = new ScheduledNotifier(this, controller, tasksTimes);
        scheduledNotifier.startScheduling();

        exchangeKeyboards = createKeyboards(4, 4, exchangeRateGetter.getValutes().stream().map(Valute::getCharCode).toList(), true);

        List<String> mainValutes = List.of("USD", "EUR", "GBP", "CNY", "JPY", "HKD", "KZT", "UAH", "BYN", "TRY", "CHF");
        convertKeyboards = createKeyboards(4, 4, mainValutes, true);

        subscribeKeyboards = createKeyboards(4, 4, mainValutes, true);

        List<String> dynamicsPeriods =
                List.of("1 неделя", "2 недели", "3 недели", "1 месяц", "2 месяца", "3 месяца",
                        "6 месяцев", "1 год", "2 года", "3 года", "5 лет", "7 лет", "10 лет");
        dynamicPeriodsKeyboards = createKeyboards(4, 4, dynamicsPeriods, false);

        dynamicValuteChoiceKeyboards = createKeyboards(4, 4, mainValutes, true);

        personalSubscriptionsKeyboardsLists = new HashMap<>();

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

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        //обработка CallbackQuery от нажатия кнопок
        if (update.hasCallbackQuery()) {
            callbackQueryHandler(update);
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            long userId = message.getFrom().getId();
            if (message.isCommand()) {
                commandsHandler(message);
            } else {
                handleMessage(userId, message);
            }
        }
    }

    private void callbackQueryHandler(Update update) {
        String data = update.getCallbackQuery().getData();
        long userId = update.getCallbackQuery().getFrom().getId();
        try {
            if (data.contains("Next") || data.contains("Back")) {
                buttonTap(update.getCallbackQuery());
            } else if (data.split("_").length == 2) {
                int keyboardId = Integer.parseInt(data.split("_")[1]);
                String body = data.split("_")[0];

                //если пришло с клавиатуры курсов валют 1-3
                int keyboardListRange = exchangeKeyboards.size();
                if (keyboardId <= keyboardListRange) {
                    sendMessage(userId, exchangeRateGetter.getSpecificExchangeRate(body));
                    return;
                }

                //если пришло от клавиатуры конвертации в валюту 4
                keyboardListRange += convertKeyboards.size();
                if (keyboardId <= keyboardListRange) {
                    sendMessage(userId, exchangeRateGetter.convert(controller.getConvertRequests().get(userId), body));
                    return;
                }

                //если пришло от клавиатуры подписки 5
                keyboardListRange += subscribeKeyboards.size();
                if (keyboardId <= keyboardListRange) {
                    if (controller.isSigned(userId, body)) {
                        sendMessage(userId, "Вы уже подписаны на " + body);
                    } else {
                        controller.addSubscription(userId, body);
                        sendMessage(userId, "Вы подписались на " + body);
                    }
                    return;
                }

                //если пришло от клавиатуры периодов динамики 6
                keyboardListRange += dynamicPeriodsKeyboards.size();
                if (keyboardId <= keyboardListRange) {
                    if (body.split(" ").length == 2) {
                        controller.getDynamicPeriodRequests().put(userId, body);
                        sendMenu(userId, "<b>Выберите валюту для отображения динамики:</b>",
                                dynamicValuteChoiceKeyboards.get(0));
                    }
                    return;
                }

                //если пришло от клавиатуры выбора валюты для вывода динамики 7
                keyboardListRange += dynamicValuteChoiceKeyboards.size();
                if (keyboardId <= keyboardListRange) {
                    String[] periodRequest = controller.getDynamicPeriodRequests().get(userId).split(" ");
                    short unit = Short.parseShort(periodRequest[0]);
                    String timeUnit = periodRequest[1];
                    sendMessage(userId, exchangeRateGetter.getDynamics(timeUnit, unit, body));
                    return;
                }

                //todo создается в процессе выполнения программы, поэтому этот блок оставлять ПОСЛЕДНИМ
                //если пришло от клавиатуры отписки 8
                keyboardListRange += personalSubscriptionsKeyboardsLists.get(userId).size();
                if (keyboardId <= keyboardListRange) {
                    controller.removeSubscription(userId, body);
                    sendMessage(userId, "Вы отписались от " + body);
                    return;
                }
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод обработки сообщений, содержащих ключевые фразы
     *
     * @param userId  id пользователя
     * @param message сообщение
     */
    private void handleMessage(long userId, Message message) {
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
                .parseMode("HTML")
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
     * @param message сообщение
     */
    private void commandsHandler(Message message) throws IOException {
        String command = message.getText();
        long userId = message.getFrom().getId();

        if (command.contains("/convert")) {
            String[] convertArr = command.split(" ");
            if (convertArr.length != 2 || !convertArr[1].matches(DOUBLE_REGEX)) { //2 - команда + сумма рублей
                sendMessage(userId, "Пример правильного запроса для конвертации: \n" +
                        "\"/convert 1000\"");
            } else {
                controller.getConvertRequests().put(userId, Double.parseDouble(convertArr[1]));
                sendMenu(userId, "<b>Выберите валюту для конвертации</b>", convertKeyboards.get(0));
            }
        } else if (command.equals("/exchange_rates")) sendMessage(userId, exchangeRateGetter.getBasicExchangeRates());
        else if (command.equals("/exchange_rates_all")) sendMessage(userId, exchangeRateGetter.getAllExchangeRates());
        else if (command.equals("/choose_currency"))
            sendMenu(userId, "<b>Выберите валюту</b>", (exchangeKeyboards.get(0)));
        else if (command.equals("/info")) sendMessage(userId, getInformation());
        else if (command.equals("/start")) {
            //сохранение общих данных пользователя в БД
            if (!controller.isContained(userId)) {
                addUser(message);
            }
            sendMenu(userId, "Welcome", replyKeyboardMarkup);
        } else if (command.contains("/feedback")) {
            String[] feedbackArr = command.split(" ");
            if (feedbackArr.length < 2) {
                sendMessage(userId, prepareFeedbackMessage());
            } else {
                String msg = command.replace("/feedback", "").trim();
                controller.addFeedback(userId, msg);

                String devId = Files.readString(Paths.get("src/main/resources/myId.txt"));
                //сообщение разработчику
                sendMessage(Long.parseLong(devId), "feedback от " + message.getFrom().getUserName() + ": " + msg);

                sendMessage(userId, "Спасибо за обратную связь!");
            }
        } else if (command.equals("/subscribe")) {
            sendMenu(userId, "<b>Выберите валюту для подписки</b>", subscribeKeyboards.get(0));
        } else if (command.equals("/unsubscribe")) {
            List<String> userCurrentSubscriptions = controller.get(userId).getSubscriptions()
                    .stream().map(Subscription::getCharCode).toList();
            if (userCurrentSubscriptions.isEmpty()){
                sendMessage(userId, "У вас пока нет подписок...");
            } else {
                List<InlineKeyboardMarkup> personalSubscriptionKeyboards =
                        createKeyboards(4, 4, userCurrentSubscriptions, true);
                personalSubscriptionsKeyboardsLists.put(userId, personalSubscriptionKeyboards);
                sendMenu(userId, "<b>Выберите валюту для отписки</b>", personalSubscriptionsKeyboardsLists.get(userId).get(0));
            }
        } else if (command.equals("/dynamics")) {
            sendMenu(userId, "<b>Выберите период времени:</b>", dynamicPeriodsKeyboards.get(0));
        } else sendMessage(userId, "Unknown command!");
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
            case "Next_1", "Back_3" -> {
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup(exchangeKeyboards.get(1));
            }
            case "Next_2" -> {
                newTxt.setText("<b>Выберите валюту</b>");
                newKb.setReplyMarkup((exchangeKeyboards.get(2)));
            }
            case "Back_2" -> {
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
     * Метод создания списка клавиатур
     *
     * @param rows         количество строк
     * @param columns      количество колонок
     * @param charCodesAll список кодов валют
     * @return
     */
    private List<InlineKeyboardMarkup> createKeyboards(int rows, int columns, List<String> charCodesAll, boolean needForFlags) {
        List<InlineKeyboardMarkup> keyboards = new ArrayList<>();
        int keyboardsQuantity = getKeyboardsQuantity(rows, columns, charCodesAll);
        int subListStartPosition = 0;
        int subListEndPosition = 0;
        for (int i = 0; i < keyboardsQuantity; i++) {
            //если клавиатура первая и не является единственной - кнопка Next нужна
            boolean needNextButton = i != keyboardsQuantity - 1;
            //если клавиатура последняя и не единственная - кнопка Back нужна
            boolean needBackButton = i != 0;
            int valuteButtonsQuantity = getValuteButtonsQuantity(rows, columns, needNextButton, needBackButton);
            subListEndPosition += valuteButtonsQuantity;
            //если это последняя клавиатуры, то границей будет - размер списка
            if (subListEndPosition >= charCodesAll.size()) subListEndPosition = charCodesAll.size();
            List<String> charCodesSublist = charCodesAll.subList(subListStartPosition, subListEndPosition);
            keyboards.add(createKeyboard(columns, rows, charCodesSublist, keyboardsTotalCounter++,
                    needNextButton, needBackButton, needForFlags));
            subListStartPosition += valuteButtonsQuantity;
        }
        return keyboards;
    }

    /**
     * Метод для расчета необходимого количества клавиатур относительно имеющихся Валют
     *
     * @param rows         строки
     * @param columns      колонки
     * @param charCodesAll список имеющихся Валют
     * @return количество клавиатур
     */
    private static int getKeyboardsQuantity(int rows, int columns, List<String> charCodesAll) {
        int edgeKeyboardButtonMaxQuantity = rows * columns - 1; //1 место для кнопки Next ИЛИ Back
        int middleKeyboardButtonMaxQuantity = rows * columns - 2; //2 места для кнопок Next И Back
        int keyboardsQuantity;
        if (charCodesAll.size() <= rows * columns) {
            keyboardsQuantity = 1;
        } else if (charCodesAll.size() <= edgeKeyboardButtonMaxQuantity * 2) {
            keyboardsQuantity = 2;
        } else {
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
     * @param rowSize        количество кнопок в строке
     * @param rowsQuantity   количество строк
     * @param charCodeList   список с кодами валют
     * @param needNextButton последняя ли это клавиатура (от этого зависит наличие кнопки Next или Back)
     * @return клавиатуру с кнопками
     */
    private InlineKeyboardMarkup createKeyboard(int rowSize, int rowsQuantity, List<String> charCodeList, int keyBoardNumb, boolean needNextButton, boolean needBackButton, boolean needForFlags) {
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
                    if (needForFlags) {
                        rowButtons.add(createCurrencyButton(charCodeList.get(k), keyBoardNumb));
                    } else {
                        rowButtons.add(createInlineButton(charCodeList.get(k), keyBoardNumb));
                    }
                }
                //когда элементы в списке заканчиваются - добавляем кнопки Next/Back
                else {
                    if (needBackButton && needNextButton) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                        rowButtons.add(createInlineButton("Next", keyBoardNumb));
                    } else if (needBackButton) {
                        rowButtons.add(createInlineButton("Back", keyBoardNumb));
                    } else if (needNextButton) {
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
     *
     * @param rowSize        кнопок в строке
     * @param rowsQuantity   количество строк
     * @param needNextButton является ли клавиатура крайней
     * @param needBackButton является ли клавиатура первой
     * @return колич-во свободных кнопок с учетом места под кнопки перехода
     */
    private static int getValuteButtonsQuantity(int rowSize, int rowsQuantity, boolean needNextButton, boolean needBackButton) {
        //если это единственная клавиатура (кнопки Next и Back не нужны)
        if (needBackButton && needNextButton) return rowsQuantity * rowSize - 2;
            //если это первая или последняя клавиатура - резерв под одну кнопку Next или Back
        else if (needBackButton || needNextButton) return rowsQuantity * rowSize - 1;
            //если это промежуточная клавиатура - нужны обе кнопки Next и Back
        else return rowsQuantity * rowSize;
    }

    /**
     * Метод создания кнопки валюты с флагом
     *
     * @param charCode     код валюты
     * @param keyBoardNumb принадлежность кнопки (для отслеживания callbackData с разных клавиатур от разных команд)
     * @return
     */
    private InlineKeyboardButton createCurrencyButton(String charCode, int keyBoardNumb) {
        return InlineKeyboardButton.builder()
                .text(charCode + Flag.flags.get(charCode))
                .callbackData(charCode + "_" + keyBoardNumb)
                .build();
    }

    /**а
     * Метод для создания кнопки с текстом
     *
     * @param keyBoardNumb принадлежность кнопки (для отслеживания callbackData с разных клавиатур от разных команд)
     * @param text         текст кнопки
     */
    private InlineKeyboardButton createInlineButton(String text, int keyBoardNumb) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(text + "_" + keyBoardNumb)
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
                "/exchange_rates - котировки топ 5 валют Мира\n" +
                "/exchange_rates_all - котировки всех валют Центробанка РФ\n" +
                "/choose_currency - выбрать конкретную валюту\n" +
                "/convert 1000 - конвертация в валюту по текущему курсу\n" +
                "/subscribe - подписаться на рассылку курсов валют по открытию/закрытию\n" +
                "Московской валютной биржи: 10:00 и 19:00 (будни)\n" +
                "/unsubscribe - отписаться от валюты\n" +
                "/dynamics - динамика курса валют за выбранный период\n" +
                "/feedback сообщение - обратная связь\n" +
                "/info - о боте";
    }

    /**
     * Метод создания и добавления нового пользователя на основе его Message
     *
     * @param message сообщение
     */
    private void addUser(Message message) {
        CustomUser customUser = new CustomUser
                .Builder()
                .withId(message.getFrom().getId())
                .withFirstName(message.getFrom().getFirstName())
                .withNickName(message.getFrom().getUserName())
                .build();
        controller.add(customUser);
    }

    private String prepareFeedbackMessage() {
        return "Связаться с разработчиком: timofeev.vadim.96@mail.ru\n" +
                "---------------------------------------\n" +
                "Чтобы оставить отзыв/пожелание/идею, отправьте сообщение через пробел после команды, " +
                "по примеру: \n" +
                "\"/feedback ваш текст\"";
    }
}

