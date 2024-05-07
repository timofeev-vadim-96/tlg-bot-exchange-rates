package org.example.dao;

import org.assertj.core.api.Assertions;
import org.example.model.User.CustomUser;
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
    private static CustomUser customUser;

    @BeforeAll
    static void setUp() {
        dao = new DaoImpl();

        customUser = new CustomUser.Builder()
                .withFirstName("testFirstName")
                .withNickName("testNickName")
                .withId((Long.parseLong( "12345")))
                .build();
    }

    @Test
    @Order(1)
    void save() {
        dao.save(customUser);
        CustomUser queryCustomUser;
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select * from telegramBotDB.users where id = " + customUser.getId()).addEntity(CustomUser.class);
            queryCustomUser = (CustomUser) nativeQuery.getSingleResult();
        }

        assertEquals(customUser, queryCustomUser);
    }

    @Test
    @Order(2)
    void get() {
        CustomUser receivedCustomUser = dao.get(customUser.getId(), CustomUser.class);

        assertEquals(customUser, receivedCustomUser);
    }

    @Test
    @Order(3)
    void update() {
        String phone = "+79851112233";
        customUser.setPhone(phone);

        dao.update(customUser);
        String queryUserPhone;
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select phone from telegramBotDB.users where id = " + customUser.getId());
            queryUserPhone = (String) nativeQuery.getSingleResult();
        }

        assertEquals(phone, queryUserPhone);
    }

    @Test
    @Order(4)
    void findAll() {
        List<CustomUser> customUsers = dao.findAll(CustomUser.class);

        assertFalse(customUsers.isEmpty());
    }

    @Test
    @Order(5)
    void deleteEntity() {
        dao.delete(customUser);

        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery
                    ("Select * from telegramBotDB.users where id = " + customUser.getId());

            Assertions.assertThatThrownBy(() -> nativeQuery.getSingleResult()).isInstanceOf(NoResultException.class);
        }
    }

    @Test
    @Order(6)
    void deleteByClassAndId() {
        dao.save(customUser);
        dao.delete(CustomUser.class, customUser.getId());

        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            NativeQuery nativeQuery = session.createNativeQuery("Select * from telegramBotDB.users where id = " + customUser.getId());

            Assertions.assertThatThrownBy(() -> nativeQuery.getSingleResult()).isInstanceOf(NoResultException.class);
        }
    }
}