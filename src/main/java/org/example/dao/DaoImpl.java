package org.example.dao;

import org.example.util.HibernateSessionFactoryUtil;
import org.example.util.Initializer;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class DaoImpl implements Dao {

    public DaoImpl() {
        Initializer.init();
    }

    /**
     * Метод поиска сущности
     * @param id идентификатор сущности
     * @param clazz тип
     * @return сущность
     */
    @Override
    public <T> T get(long id, Class<T> clazz) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            return session.get(clazz, id);
        }
    }

    /**
     * Метод сохранения сущности в БД
     * @param t сущность
     * @param <T> ее тип
     */
    @Override
    public <T> void save(T t) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.save(t);
            tx1.commit();
        }
    }

    /**
     * Метод обновления сущности
     * @param t сущность
     * @param <T> ее тип
     */
    @Override
    public <T> void update(T t) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.update(t);
            tx1.commit();
        }
    }

    /**
     * Метод удаления сущности
     * @param t сущность
     * @param <T> тип
     */
    @Override
    public <T> void delete(T t) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.delete(t);
            tx1.commit();
        }
    }

    /**
     * Метод удаления сущности по id
     * @param clazz тип
     * @param id идентификатор сущности
     */
    @Override
    public <T> void delete(Class<T> clazz, long id) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            Transaction tx1 = session.beginTransaction();
            session.delete(session.get(clazz, id));
            tx1.commit();
        }
    }

    /**
     * Метод поиска всех элементов указанного типа
     * @param clazz тип
     * @return список элементов указанного типа
     */
    @Override
    public <T> List<T> findAll(Class<T> clazz) {
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()) {
            return (List<T>) session.createQuery("From " + clazz.getName()).list();
        }
    }
}