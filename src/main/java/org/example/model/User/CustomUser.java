package org.example.model.User;

import lombok.*;

import javax.persistence.*;
import java.util.*;

@Entity
@AllArgsConstructor
@Data
@Table(name = "users")
public class CustomUser {
    @Id
    private long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "nick_name")
    private String nickName;
    private String phone;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Feedback> feedbackMessages;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_subscription",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "subscription_id", referencedColumnName = "id"))
    private Set<Subscription> subscriptions;

    public CustomUser() {
        feedbackMessages = new ArrayList<>();
        subscriptions = new HashSet<>();
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
        feedback.setUser(this);
        feedbackMessages.add(feedback);
    }

    public void addFeedback(String text){
        Feedback feedback = new Feedback(text);
        feedback.setUser(this);
        feedbackMessages.add(feedback);
    }

    public void removeFeedback(Feedback feedback){
        feedbackMessages.remove(feedback);
    }

    public void removeFeedback(long tableId){
        feedbackMessages.removeIf(feedback -> feedback.getId() == tableId);
    }
    //endregion


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        CustomUser customUser = (CustomUser) object;
        return Objects.equals(firstName, customUser.firstName) && Objects.equals(nickName, customUser.nickName) && Objects.equals(phone, customUser.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, nickName, phone);
    }

    public static class Builder {
        private CustomUser customUser;

        public Builder(){
            customUser = new CustomUser();
        }

        public Builder withId (long userId){
            customUser.setId(userId);
            return this;
        }

        public Builder withFirstName (String firstName){
            customUser.setFirstName(firstName);
            return this;
        }

        public Builder withNickName (String nickName){
            customUser.setNickName(nickName);
            return this;
        }

        public Builder withPhone (String phone){
            customUser.setPhone(phone);
            return this;
        }

        public CustomUser build(){
            return customUser;
        }
    }
}
