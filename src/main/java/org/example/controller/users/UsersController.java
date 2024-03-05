package org.example.controller.users;

import lombok.Getter;
import org.example.dao.Dao;
import org.example.model.User.Feedback;
import org.example.model.User.Subscription;
import org.example.model.User.UserApp;

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

    public UsersController add(UserApp userApp){
        dao.save(userApp);
        return this;
    }

    public UsersController remove(UserApp userApp){
        dao.delete(userApp);
        return this;
    }
    public UsersController remove(long telegramId){
        dao.delete(UserApp.class, telegramId);
        return this;
    }

    /**
     * Метод для получения пользователя по id телеграмма
     * @param telegramId id пользователя в телеграмме
     * @return либо User, либо null
     */
    public UserApp get(long telegramId){
        return dao.get(telegramId, UserApp.class);
    }

    public boolean isContained(long telegramId){
        List<UserApp> userApps = dao.findAll(UserApp.class);
        for (UserApp userApp : userApps){
            if (userApp.getId() == telegramId) return true;
        }
        return false;
    }

    //region Subscription
    public boolean isSigned(long telegramId, String charCode){
        UserApp userApp = get(telegramId);
        for (Subscription subscription: userApp.getSubscriptions()){
            if (subscription.getCharCode().equals(charCode)) return true;
        }
        return false;
    }

    public void addSubscription(long telegramId, String charCode){
        UserApp userApp = get(telegramId);

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

        userApp.addSubscription(subscription);
        dao.update(userApp);
    }

    public void removeSubscription(long telegramId, String charCode){
        UserApp userApp = get(telegramId);
        userApp.removeSubscription(charCode);
        dao.update(userApp);
    }
    //endregion

    // region Feedback
    public void addFeedback(long telegramId, String text){
        UserApp userApp = get(telegramId);

        Feedback feedback = new Feedback(userApp.getFirstName() + ": " + text);

        userApp.addFeedback(feedback);
        dao.update(userApp);
    }

    public void removeFeedback(long telegramId, long tableId){
        UserApp userApp = get(telegramId);
        userApp.removeFeedback(tableId);
        dao.update(userApp);
    }
    //endregion

    public List<UserApp> getUsers(){
        return dao.findAll(UserApp.class);
    }
}
