package org.example.controller.subscription;

import org.example.controller.Bot;
import org.example.controller.users.UsersController;
import org.example.model.User.Subscription;
import org.example.model.User.User;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScheduledNotifier {
    private UsersController usersController;
    private Bot bot;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final static long HOUR_IN_MILLISECONDS = 86_400_000;

    //создает пул потоков для выполнения задач
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    public ScheduledNotifier(Bot bot, UsersController usersController) {
        this.usersController = usersController;
        this.bot = bot;
    }

    /**
     * Рассылка курсов валют по открытии и закрытии Московской валютной биржи
     */
    public void startScheduling() {
        // Расписание выполнения задачи
        executor.scheduleAtFixedRate(() -> {
            schedule("<b>Добрый день! Ваша утренняя рассылка курсов валют:</b>");
        }, getDelayUntilNextExecution(10, 00), HOUR_IN_MILLISECONDS, TimeUnit.MILLISECONDS); // Запуск каждые 24 часа

        executor.scheduleAtFixedRate(() -> {
            schedule("<b>Добрый вечер! Ваша вечерняя рассылка курсов валют:</b>");
        }, getDelayUntilNextExecution(19, 0), HOUR_IN_MILLISECONDS, TimeUnit.MILLISECONDS); // Запуск каждые 24 часа
    }


    /**
     * Метод отправки уведомлений пользователям по их подпискам
     */
    private void schedule(String greeting) {
        //todo
        System.out.println("Исполнение метода отправки сообщений по ПОДПИСКЕ");
        List<User> users = usersController.getUsers();
        for (User user : users) {
            Set<String> subscriptions = user.getSubscriptions()
                    .stream()
                    .map(Subscription::getCharCode)
                    .collect(Collectors.toSet());
            if (!subscriptions.isEmpty()) {
                bot.sendMessage(user.getId(), greeting);
                for (String subscription : subscriptions) {
                    bot.sendMessage(user.getId(), bot.getExchangeRateGetter().getSpecificExchangeRate(subscription));
                }
            }
        }
    }

    /**
     * Метод определения задержки до следующего запуска (время до ближайшего запуска)
     */
    private static long getDelayUntilNextExecution(int targetHour, int targetMinute) {
        long currentTime = System.currentTimeMillis();
        long targetTime = getNextExecutionTime(targetHour, targetMinute);
        return targetTime - currentTime;
    }

    /**
     * Метод получения времени следующего выполнения с учетом текущего времени
     * @param targetHour час дня выполнения задачи
     * @param targetMinute конкретная минута выполнения задачи
     */
    private static long getNextExecutionTime(int targetHour, int targetMinute) {
        long currentTime = System.currentTimeMillis();
        Calendar targetTime = Calendar.getInstance();
        targetTime.set(Calendar.HOUR_OF_DAY, targetHour);
        targetTime.set(Calendar.MINUTE, targetMinute);
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);
        // Если время в прошлом, переносим на следующий день
        if (targetTime.getTimeInMillis() <= currentTime) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1);
        }
        return targetTime.getTimeInMillis();
    }
}


