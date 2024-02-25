package org.example.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class Initializer {
    public static void init (){
        try (Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession()){
            Transaction transaction = session.beginTransaction();
            session.createNativeQuery("create database if not exists telegramBotDB;").executeUpdate();
            session.createNativeQuery("use telegramBotDB;").executeUpdate();
            session.createNativeQuery("create table if not exists users (\n" +
                    "    id bigint primary key not null,\n" +
                    "    first_name varchar(50) not null,\n" +
                    "    nick_name varchar(50) not null,\n" +
                    "    phone varchar(20),\n" +
                    "    conversion_request double,\n" +
                    "    is_subscriber bool\n" +
                    ");").executeUpdate();
            session.createNativeQuery("create table if not exists subscriptions (\n" +
                    "    id int primary key auto_increment,\n" +
                    "    char_code varchar(5) unique not null\n" +
                    ");").executeUpdate();
            session.createNativeQuery("create table if not exists user_subscription (\n" +
                    "    user_id bigint not null,\n" +
                    "    subscription_id int not null,\n" +
                    "    foreign key (user_id) references users (id),\n" +
                    "    foreign key (subscription_id) references subscriptions (id)\n" +
                    ");").executeUpdate();
            session.createNativeQuery("create table if not exists feedbacks (\n" +
                    "    id int primary key auto_increment,\n" +
                    "    text varchar(500) not null,\n" +
                    "    user_id bigint not null,\n" +
                    "    foreign key (user_id) references users (id)\n" +
                    ");").executeUpdate();

            transaction.commit();
        }
    }
}
