package org.example.controller.users;

import lombok.Getter;
import org.example.dao.Dao;
import org.example.model.User.CustomUser;
import org.example.model.User.Feedback;
import org.example.model.User.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UsersController {
    private Dao dao;
    @Getter
    private Map<Long, Double> convertRequests;
    @Getter
    private Map<Long, String> dynamicPeriodRequests;

    public UsersController (Dao dao) {
        this.dao = dao;
        convertRequests = new HashMap<>();
        dynamicPeriodRequests = new HashMap<>();
    }

    public UsersController add(CustomUser customUser){
        dao.save(customUser);
        return this;
    }

    public UsersController remove(CustomUser customUser){
        dao.delete(customUser);
        return this;
    }
    public UsersController remove(long telegramId){
        dao.delete(CustomUser.class, telegramId);
        return this;
    }

    /**
     * Метод для получения пользователя по id телеграмма
     * @param telegramId id пользователя в телеграмме
     * @return либо User, либо null
     */
    public CustomUser get(long telegramId){
        return dao.get(telegramId, CustomUser.class);
    }

    public boolean isContained(long telegramId){
        List<CustomUser> customUsers = dao.findAll(CustomUser.class);
        for (CustomUser customUser : customUsers){
            if (customUser.getId() == telegramId) return true;
        }
        return false;
    }

    //region Subscription
    public boolean isSigned(long telegramId, String charCode){
        CustomUser customUser = get(telegramId);
        for (Subscription subscription: customUser.getSubscriptions()){
            if (subscription.getCharCode().equals(charCode)) return true;
        }
        return false;
    }

    public void addSubscription(long telegramId, String charCode){
        CustomUser customUser = get(telegramId);

        Subscription subscription = new Subscription(charCode);

        List<Subscription> subscriptions = dao.findAll(Subscription.class);
        //если такой валюты в базе нет - добавляем в базу
        if (!subscriptions.contains(subscription)){
            dao.save(subscription);

            //если такая валюта есть - тянем ее вместе с ее id
        } else{
            subscription = subscriptions
                    .stream()
                    .filter(sub -> sub.getCharCode().equals(charCode))
                    .findFirst().orElse(null);
        }

        customUser.addSubscription(subscription);
        dao.update(customUser);
    }

    public void removeSubscription(long telegramId, String charCode){
        CustomUser customUser = get(telegramId);
        customUser.removeSubscription(charCode);
        dao.update(customUser);
    }
    //endregion

    // region Feedback
    public void addFeedback(long telegramId, String text){
        CustomUser customUser = get(telegramId);

        Feedback feedback = new Feedback(customUser.getFirstName() + ": " + text);

        customUser.addFeedback(feedback);
        dao.update(customUser);
    }

    public void removeFeedback(long telegramId, long tableId){
        CustomUser customUser = get(telegramId);
        customUser.removeFeedback(tableId);
        dao.update(customUser);
    }
    //endregion

    public List<CustomUser> getUsers(){
        return dao.findAll(CustomUser.class);
    }
}
