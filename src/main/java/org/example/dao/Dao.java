package org.example.dao;

import java.util.List;

public interface Dao {
    public <T> void save(T t);

    public <T> void update(T t);

    public <T> void delete(T t);

    public <T> T get(long id, Class<T> clazz);

    public <T> List<T> findAll(Class<T> clazz);

    public <T> void delete(Class<T> clazz, long id);
}
