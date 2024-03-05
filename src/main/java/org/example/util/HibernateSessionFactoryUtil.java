package org.example.util;

import org.example.model.User.Feedback;
import org.example.model.User.Subscription;
import org.example.model.User.UserApp;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Класс для создания фабрики сессий для работы с БД
 */
public class HibernateSessionFactoryUtil {
    private static SessionFactory sessionFactory;

    private HibernateSessionFactoryUtil() {}

    /**
     * Метод создания фабрики сессий в единственном экземпляре
     * @return фабрику сессий
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration().configure();
                configuration.addAnnotatedClass(UserApp.class);
                configuration.addAnnotatedClass(Feedback.class);
                configuration.addAnnotatedClass(Subscription.class);
                sessionFactory = configuration.buildSessionFactory();
            } catch (Exception e) {
                System.out.println("Exception when trying to create a database session!" + e);
            }
        }
        return sessionFactory;
    }
}
