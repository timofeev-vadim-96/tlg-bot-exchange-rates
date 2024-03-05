package org.example.util;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HibernateSessionFactoryUtilTest {

    @Test
    void getSessionFactory() {
        SessionFactory sessionFactory = HibernateSessionFactoryUtil.getSessionFactory();

        assertNotNull(sessionFactory);
    }
}