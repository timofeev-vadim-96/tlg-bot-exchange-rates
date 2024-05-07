package org.example.controller.subscription;

import org.example.controller.bot.Bot;
import org.example.controller.users.UsersController;
import org.example.model.User.CustomUser;
import org.example.model.User.Subscription;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScheduledNotifier {
    private UsersController usersController;
    private Bot bot;
    private final static long HOUR_IN_MILLISECONDS = 86_400_000;
    private Map<Integer, Integer> tasksTimes;


    private ScheduledExecutorService executor;
    public ScheduledNotifier(Bot bot, UsersController usersController, Map<Integer, Integer> tasksTimes) {
        this.usersController = usersController;
        this.bot = bot;
        this.tasksTimes = tasksTimes;
        //создает пул потоков, зависящий от количества задач по временам
        executor = Executors.newScheduledThreadPool(tasksTimes.size());
    }

    /**
     * Рассылка курсов валют по открытии и закрытии Московской валютной биржи
     */
    public void startScheduling() {
        // Расписание выполнения задачи
        tasksTimes.forEach((targetHour,targetMinute)->{
            //ночь
            if (0 <= targetHour && targetHour < 4 || targetHour == 23){
                getScheduledFuture("<b>Доброй ночи! Ваша ночная рассылка курсов валют:</b>",
                        targetHour, targetMinute);
            }
            //утро
            else if (4 <= targetHour && targetHour < 11){
                getScheduledFuture("<b>Доброе утро! Ваша утренняя рассылка курсов валют:</b>",
                        targetHour, targetMinute);
            }
            //день
            else if (11 <= targetHour && targetHour < 16){
                getScheduledFuture("<b>Добрый день! Ваша дневная рассылка курсов валют:</b>",
                        targetHour, targetMinute);
            }
            //вечер
            else if (16 <= targetHour && targetHour < 23){
                getScheduledFuture("<b>Добрый вечер! Ваша вечерняя рассылка курсов валют:</b>",
                        targetHour, targetMinute);
            }
        });
    }

    private ScheduledFuture<?> getScheduledFuture(String greeting, int targetHour, int targetMinute) {
        return executor.scheduleAtFixedRate(() -> {
            schedule(greeting);
        }, getDelayUntilNextExecution(targetHour, targetMinute), HOUR_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
    }


    /**
     * Метод отправки уведомлений пользователям по их подпискам
     */
    private void schedule(String greeting) {
        List<CustomUser> customUsers = usersController.getUsers();
        for (CustomUser customUser : customUsers) {
            Set<String> subscriptions = customUser.getSubscriptions()
                    .stream()
                    .map(Subscription::getCharCode)
                    .collect(Collectors.toSet());
            if (!subscriptions.isEmpty()) {
                bot.sendMessage(customUser.getId(), greeting);
                for (String subscription : subscriptions) {
                    bot.sendMessage(customUser.getId(), bot.getExchangeRateGetter().getSpecificExchangeRate(subscription));
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


