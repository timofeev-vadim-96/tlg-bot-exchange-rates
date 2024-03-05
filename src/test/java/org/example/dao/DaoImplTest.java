package org.example.dao;

import org.assertj.core.api.Assertions;
import org.example.model.User.UserApp;
import org.example.util.HibernateSessionFactoryUtil;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.*;

import javax.persistence.NoResultException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Для корректной работы тестов необходимо подключенная БД MySql
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DaoImplTest {
    private static Dao dao;
    private static UserApp userApp;

    @BeforeAll
    static void setUp() {
        dao = new DaoImpl();

        userApp = new UserApp.Builder()
                .withFirstName("testFirstName")
                .withNickName("testNickName")
                .withId((Long.parseLong( "12345")))
                .build();
    }

    @Test
    @Order(1)
    void save() {
        dao.save(userApp);
        UserApp queryUserApp;
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select * from telegramBotDB.users where id = " + userApp.getId()).addEntity(UserApp.class);
            queryUserApp = (UserApp) nativeQuery.getSingleResult();
        }

        assertEquals(userApp, queryUserApp);
    }

    @Test
    @Order(2)
    void get() {
        UserApp receivedUserApp = dao.get(userApp.getId(), UserApp.class);

        assertEquals(userApp, receivedUserApp);
    }

    @Test
    @Order(3)
    void update() {
        String phone = "+79851112233";
        userApp.setPhone(phone);

        dao.update(userApp);
        String queryUserPhone;
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select phone from telegramBotDB.users where id = " + userApp.getId());
            queryUserPhone = (String) nativeQuery.getSingleResult();
        }

        assertEquals(phone, queryUserPhone);
    }

    @Test
    @Order(4)
    void findAll() {
        List<UserApp> userApps = dao.findAll(UserApp.class);

        assertFalse(userApps.isEmpty());
    }

    @Test
    @Order(5)
    void deleteEntity() {
        dao.delete(userApp);

        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select * from telegramBotDB.users where id = " + userApp.getId());

            Assertions.assertThatThrownBy(() -> nativeQuery.getSingleResult()).isInstanceOf(NoResultException.class);
        }
    }

    @Test
    @Order(6)
    void deleteByClassAndId() {
        dao.save(userApp);
        dao.delete(UserApp.class, userApp.getId());

        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery("Select * from telegramBotDB.users where id = " + userApp.getId());

            Assertions.assertThatThrownBy(() -> nativeQuery.getSingleResult()).isInstanceOf(NoResultException.class);
        }
    }
}