package org.example.model.User;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@Data
@Table(name = "users")
public class User {
    @Id
    private long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "nick_name")
    private String nickName;
    private String phone;
    @OneToMany(mappedBy = "id", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Feedback> feedbackMessages;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_subscription",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "subscription_id", referencedColumnName = "id"))
    private Set<Subscription> subscriptions;

    public User() {
        feedbackMessages = new ArrayList<>();
    }

    //region Subscription
    public void addSubscription(Subscription subscription){
        subscriptions.add(subscription);
    }

    public void addSubscription(String charCode){
        Subscription subscription = new Subscription(charCode);
        subscriptions.add(subscription);
    }

    public void removeSubscription(Subscription subscription){
        subscriptions.remove(subscription);
    }

    public void removeSubscription(String charCode){
        subscriptions.removeIf(subscription -> subscription.getCharCode().equals(charCode));
    }
    //endregion

    //region Feedback
    public void addFeedback(Feedback feedback){
        feedbackMessages.add(feedback);
    }

    public void addFeedback(String text){
        Feedback feedback = new Feedback(text);
        feedbackMessages.add(feedback);
    }

    public void removeFeedback(Feedback feedback){
        feedbackMessages.remove(feedback);
    }

    public void removeFeedback(int tableId){
        feedbackMessages.removeIf(feedback -> feedback.getId() == tableId);
    }
    //endregion

    public static class Builder {
        private User user;

        public Builder(){
            user = new User();
        }

        public Builder withId (long userId){
            user.setId(userId);
            return this;
        }

        public Builder withFirstName (String firstName){
            user.setFirstName(firstName);
            return this;
        }

        public Builder withNickName (String nickName){
            user.setNickName(nickName);
            return this;
        }

        public Builder withPhone (String phone){
            user.setPhone(phone);
            return this;
        }

        public User build(){
            return user;
        }
    }
}
