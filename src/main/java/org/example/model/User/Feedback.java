package org.example.model.User;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@Table(name = "feedbacks")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String text;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private CustomUser user;

    public Feedback(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Feedback feedback = (Feedback) object;
        return Objects.equals(text, feedback.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
