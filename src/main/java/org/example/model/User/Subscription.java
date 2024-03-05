package org.example.model.User;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter
@Setter
@ToString
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "char_code")
    private String charCode; //valute
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_subscription",
            joinColumns = @JoinColumn(name = "subscription_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    private Set<UserApp> userApps;

    public Subscription() {
        userApps = new HashSet<>();
    }

    public Subscription(String charCode) {
        this.charCode = charCode;
        userApps = new HashSet<>();
    }

    @Override
    public boolean equals(Object obj) {
        Subscription subscription = (Subscription) obj;
        return subscription.getCharCode().equals(this.charCode);
    }

    @Override
    public int hashCode() {
        return id;
    }
}
