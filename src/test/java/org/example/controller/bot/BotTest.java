package org.example.controller.bot;

import org.example.controller.users.UsersController;
import org.example.model.User.CustomUser;
import org.example.model.cbr.Valute;
import org.example.services.exchangeRates.ExchangeRateGetter;
import org.example.util.Flag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotTest {
    @Spy
    private static Bot bot;
    private static ExchangeRateGetter exchangeRateGetter;
    private static UsersController controller;

    @BeforeAll
    static void setUp() throws IOException {
        String token = Files.readString(Paths.get("src/main/resources/token.config"));
        exchangeRateGetter = mock(ExchangeRateGetter.class);
        controller = mock(UsersController.class);

        //exchangeKeyboards бота инициализируется в конструкторе на основе возвращаемого списка
        //от exchangeRateGetter.getValutes
        List<Valute> valutes = getValutes();
        when(exchangeRateGetter.getValutes()).thenReturn(valutes);

        bot = new Bot(token, exchangeRateGetter, controller);
    }

    private static List<Valute> getValutes() {
        List<Valute> valutes = new ArrayList<>();
        for (String key : Flag.flags.keySet()) {
            Valute valute = new Valute();
            valute.setCharCode(key);
            valutes.add(valute);
        }
        return valutes;
    }

    @BeforeEach
    void resetMock() {
        reset(controller, exchangeRateGetter, bot);
    }

    @Test
    void getBotUsername() {
        String botUserName = "w0nder_waffle_bot";

        assertEquals(botUserName, bot.getBotUsername());
    }

    @Test
    void getBotToken() throws IOException {
        String token = Files.readString(Paths.get("src/main/resources/token.config"));

        assertEquals(token, bot.getBotToken());
    }

    /**
     * Тест сценария команды /info
     */
    @Test
    void infoCommand() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/info");
        doNothing().when(bot).sendMessage(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария команды /start
     */
    @Test
    void startCommand() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/start");
        doNothing().when(bot).sendMenu(anyLong(), anyString(), any(ReplyKeyboard.class));
        when(controller.isContained(userId)).thenReturn(true);

        bot.onUpdateReceived(update);

        verify(controller, times(1)).isContained(userId);
        verify(bot, times(1)).sendMenu(eq(userId), anyString(), any(ReplyKeyboard.class));
    }

    /**
     * Тест сценария прихода неизвестной команды
     */
    @Test
    void unknownCommand() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/anyUnknownCommand");
        doNothing().when(bot).sendMessage(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(bot, times(1)).sendMessage(eq(userId), eq("Unknown command!"));
    }

    /**
     * Тест сценария команд /dynamics, /subscribe, /choose_currency
     */
    @ParameterizedTest
    @ValueSource(strings = {"/dynamics", "/subscribe", "/choose_currency"})
    void dynamicsSubscribeChooseCurrencyCommands(String command) {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, command);
        doNothing().when(bot).sendMenu(anyLong(), anyString(), any(ReplyKeyboard.class));

        bot.onUpdateReceived(update);

        verify(bot, times(1)).sendMenu(eq(userId), anyString(), any(ReplyKeyboard.class));
    }

    /**
     * Тест сценария команды /exchange_rates
     */
    @Test
    void exchangeRatesCommand() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/exchange_rates");
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(exchangeRateGetter.getBasicExchangeRates()).thenReturn("basic currencies exchange rates");

        bot.onUpdateReceived(update);

        verify(exchangeRateGetter, times(1)).getBasicExchangeRates();
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария команды /exchange_rates_all
     */
    @Test
    void exchangeRatesAllCommand() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/exchange_rates_all");
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(exchangeRateGetter.getAllExchangeRates()).thenReturn("returned all exchange rates");

        bot.onUpdateReceived(update);

        verify(exchangeRateGetter, times(1)).getAllExchangeRates();
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария команды /convert с неправильным аргументом
     */
    @ParameterizedTest
    @ValueSource(strings = {"/convert", "/convert incorrect_value", "/convert 12abc34"})
    void convertCommandWithoutIncorrectValue(String command) {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, command);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        String answer = "Пример правильного запроса для конвертации: \n" +
                "\"/convert 1000\"";

        bot.onUpdateReceived(update);

        verify(bot, times(1)).sendMessage(eq(userId), eq(answer));
    }

    /**
     * Тест сценария команды /convert с корректным аргументом
     */
    @Test
    void convertCommandWithCorrectValue() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/convert 1234");
        doNothing().when(bot).sendMenu(anyLong(), anyString(), any(ReplyKeyboard.class));
        when(controller.getConvertRequests()).thenReturn(new HashMap<>());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).getConvertRequests();
        verify(bot, times(1)).sendMenu(eq(userId), anyString(), any(ReplyKeyboard.class));
    }

    /**
     * Тест сценария команды /unsubscribe в случае, когда у пользователя отсутствуют подписки
     */
    @Test
    void unsubscribeCommandWhenThereAreNoSubscribes() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/unsubscribe");
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(controller.get(userId)).thenReturn(new CustomUser());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).get(userId);
        verify(bot, times(1)).sendMessage(eq(userId), eq("У вас пока нет подписок..."));
    }

    /**
     * Тест сценария команды /unsubscribe в случае, когда у пользователя подписки присутствуют
     */
    @Test
    void unsubscribeCommandWhenHeHasSubscribes() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/unsubscribe");
        doNothing().when(bot).sendMenu(anyLong(), anyString(), any(ReplyKeyboard.class));
        CustomUser customUser = new CustomUser();
        customUser.addSubscription("USD");
        when(controller.get(userId)).thenReturn(customUser);

        bot.onUpdateReceived(update);

        verify(controller, times(1)).get(userId);
        verify(bot, times(1)).sendMenu(eq(userId), anyString(), any(ReplyKeyboard.class));
    }

    /**
     * Тест сценария команды /feedback без текстовой части обратной связи
     */
    @Test
    void feedbackCommandWhenWithoutText() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/feedback");
        doNothing().when(bot).sendMessage(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    @Test
    void feedbackCommandWhenHasText() {
        long userId = 1;
        Update update = createUpdateWithCommandInside(userId, "/feedback some feedback");
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        doNothing().when(controller).addFeedback(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).addFeedback(eq(userId), anyString());
        verify(bot, times(1)).sendMessage(eq(userId), eq("Спасибо за обратную связь!"));
    }

    private static Update createUpdateWithCommandInside(long userId, String command) {
        User user = new User();
        user.setId((userId));

        //значение length не имеет значение для типа bot_command
        MessageEntity commandEntity = new MessageEntity("bot_command", 0, 0);

        // Создание списка сущностей и добавление сущности "bot_command"
        List<MessageEntity> entities = new ArrayList<>();
        entities.add(commandEntity);
        Message message = new Message();
        message.setEntities(entities);
        message.setText(command);
        message.setFrom(user);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static Update getUpdateWithCallbackQuery(String string, long id) {
        User user = new User();
        user.setId(id);
        CallbackQuery query = new CallbackQuery();
        query.setData(string);
        query.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(query);
        return update;
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатур курсов валют (## 1-3 - индекс клавиатуры по логике архитектуры)
     */
    @ParameterizedTest
    @ValueSource(strings = {"USD_1", "EUR_2", "CNY_3"})
    void callbackQueryFromExchangeKeyboards(String data) {
        long userId = 1;
        Update update = getUpdateWithCallbackQuery(data, userId);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(exchangeRateGetter.getSpecificExchangeRate(anyString()))
                .thenReturn("returned specific exchange rate");

        bot.onUpdateReceived(update);

        verify(exchangeRateGetter, times(1)).getSpecificExchangeRate(anyString());
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатур конвертации валюты (# 4)
     */
    @Test
    void callbackQueryFromConvertKeyboards() {
        long userId = 1;
        Update update = getUpdateWithCallbackQuery("USD_4", userId);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(controller.getConvertRequests()).thenReturn(Map.of(userId, 1000.00));
        when(exchangeRateGetter.convert(anyDouble(), anyString())).thenReturn("converted value");

        bot.onUpdateReceived(update);

        verify(controller, times(1)).getConvertRequests();
        verify(exchangeRateGetter, times(1)).convert(eq(1000.00), eq("USD"));
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатур подписки на валюты (# 5)
     */
    @Test
    void callbackQueryFromSubscriptionKeyboards() {
        long userId = 1;
        Update update = getUpdateWithCallbackQuery("USD_5", userId);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(controller.isSigned(userId, "USD")).thenReturn(false);
        doNothing().when(controller).addSubscription(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).isSigned(eq(userId), eq("USD"));
        verify(controller, times(1)).addSubscription(eq(userId), eq("USD"));
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатур периодов динамики (# 6)
     */
    @Test
    void callbackQueryFromDynamicPeriodsKeyboards() {
        long userId = 1;
        Update update = getUpdateWithCallbackQuery("1 месяц_6", userId);
        doNothing().when(bot).sendMenu(anyLong(), anyString(), any(ReplyKeyboard.class));
        when(controller.getDynamicPeriodRequests()).thenReturn(new HashMap<>());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).getDynamicPeriodRequests();
        verify(bot, times(1)).sendMenu(eq(userId), anyString(), any(ReplyKeyboard.class));
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатуры выбора валюты для вывода динамики (# 7)
     */
    @Test
    void callbackQueryFromDynamicValuteChoiceKeyboards() {
        long userId = 1;
        Update update = getUpdateWithCallbackQuery("USD_7", userId);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        when(controller.getDynamicPeriodRequests()).thenReturn(Map.of(userId, "1 месяц"));
        when(exchangeRateGetter.getDynamics("месяц", (short) 1, "USD"))
                .thenReturn("returned specific currencies dynamics");

        bot.onUpdateReceived(update);

        verify(controller, times(1)).getDynamicPeriodRequests();
        verify(exchangeRateGetter, times(1)).getDynamics(anyString(), anyShort(), eq("USD"));
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }

    /**
     * Тест сценария прихода callbackQuery от клавиатуры отписки (# 8). Чтобы список текущих клавиатур создался,
     * необходимо прогнать сценарий команды /unsubscribe
     */
    @Test
    void callbackQueryFromPersonalSubscriptionsKeyboardsLists() {
        unsubscribeCommandWhenHeHasSubscribes();
        long userId = 1;
        Update update = getUpdateWithCallbackQuery("USD_8", userId);
        doNothing().when(bot).sendMessage(anyLong(), anyString());
        doNothing().when(controller).removeSubscription(anyLong(), anyString());

        bot.onUpdateReceived(update);

        verify(controller, times(1)).removeSubscription(eq(userId), eq("USD"));
        verify(bot, times(1)).sendMessage(eq(userId), anyString());
    }
}