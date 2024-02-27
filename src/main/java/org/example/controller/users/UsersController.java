package org.example.controller.users;

import lombok.Getter;
import org.example.controller.Bot;
import org.example.controller.subscription.ScheduledNotifier;
import org.example.dao.Dao;
import org.example.model.User.Subscription;
import org.example.model.User.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsersController {
    Dao dao;
    @Getter
    Map<Long, Double> convertRequests;

    public UsersController (Dao dao) {
        this.dao = dao;
        convertRequests = new HashMap<>();
    }

    public UsersController add(User user){
        dao.save(user);
        return this;
    }

    public UsersController remove(User user){
        dao.delete(user);
        return this;
    }
    public UsersController remove(int telegramId){
        dao.delete(User.class, telegramId);
        return this;
    }

    /**
     * Метод для получения пользователя по id телеграмма
     * @param telegramId id пользователя в телеграмме
     * @return либо User, либо null
     */
    public User get(long telegramId){
        return dao.get(telegramId, User.class);
    }

    public boolean isContained(long telegramId){
        List<User> users = dao.findAll(User.class);
        for (User user: users){
            if (user.getId() == telegramId) return true;
        }
        return false;
    }

    //region Subscription
    public boolean isSigned(long telegramId, String charCode){
        User user = get(telegramId);
        for (Subscription subscription: user.getSubscriptions()){
            if (subscription.getCharCode().equals(charCode)) return true;
        }
        return false;
    }

    public void addSubscription(long telegramId, String charCode){
        User user = get(telegramId);

        Subscription subscription = new Subscription(charCode);
        System.out.println(dao.findAll(Subscription.class).contains(subscription));

        //если такой подписки в базе нет - добавляем в базу
        if (!dao.findAll(Subscription.class).contains(subscription)){
            dao.save(subscription);

            //если такая подписка есть - тянем ее вместе с ее id
        } else{
            subscription = dao.findAll(Subscription.class)
                    .stream()
                    .filter(sub -> sub.getCharCode().equals(charCode))
                    .findFirst().orElse(null);
        }

        user.addSubscription(subscription);
        dao.update(user);
    }

    public void removeSubscription(long telegramId, String charCode){
        User user = get(telegramId);
        user.removeSubscription(charCode);
        dao.update(user);
    }
    //endregion

    // region Feedback
    public void addFeedback(long telegramId, String text){
        User user = get(telegramId);
        user.addFeedback(text);
        dao.update(user);
    }

    public void removeFeedback(long telegramId, int tableId){
        User user = get(telegramId);
        user.removeFeedback(tableId);
        dao.update(user);
    }
    //endregion

    public List<User> getUsers(){
        return dao.findAll(User.class);
    }
}
