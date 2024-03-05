package org.example.controller.subscription;

import org.example.controller.bot.Bot;
import org.example.controller.users.UsersController;
import org.example.model.User.UserApp;
import org.example.services.exchangeRates.ExchangeRateGetter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class ScheduledNotifierTest {

    /**
     * Тест, проверяющий, что рассылка просходит в заданное время, а также то, что совокупность приватных
     * методов в классе, в итоге, обращаются к внешним зависимостям и вызывают необходимые методы
     *
     * @throws InterruptedException
     */
    @Test
    void startScheduling() throws InterruptedException {
        Bot bot = mock(Bot.class);
        UsersController usersController = mock(UsersController.class);
        LocalDateTime testDateTime = LocalDateTime.now().plusSeconds(61);
        int hour = testDateTime.getHour();
        int minute = testDateTime.getMinute();
        Map<Integer, Integer> tasksTimes = Map.of(hour, minute);
        ScheduledNotifier scheduledNotifier = new ScheduledNotifier(bot, usersController, tasksTimes);
        UserApp userApp = new UserApp();
        userApp.addSubscription("USD");
        when(usersController.getUsers()).thenReturn(List.of(userApp));
        when(bot.getExchangeRateGetter()).thenReturn(mock(ExchangeRateGetter.class));

        scheduledNotifier.startScheduling();
        Thread.sleep(70_000);

        verify(usersController, times(1)).getUsers();
        verify(bot, atLeastOnce()).sendMessage(anyLong(), anyString());
    }
}