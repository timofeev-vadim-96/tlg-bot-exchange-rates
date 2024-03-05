package org.example.util;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Для корректной работы тестов необходимо подключенная БД MySql
 */
class TablesInitializerTest {

    /**
     * Тест определения наличия необходимых таблиц в БД после выполнения метода
     */
    @Test
    void init() {
        Set<String> expectedSet = Set.of("subscriptions", "user_subscription", "users", "feedbacks");

        TablesInitializer.init();
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Show tables from telegramBotDB;");
            Set<String> tables = (Set<String>) nativeQuery.getResultList().stream().collect(Collectors.toSet());

            assertEquals(expectedSet.size(), tables.size());
        }
    }
}