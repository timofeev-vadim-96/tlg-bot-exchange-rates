package org.example.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonConverterUtil {
    static Gson gson = new GsonBuilder().setPrettyPrinting().create(); //с красивым читаемым выводом в файл

    /**
     * Метод для конвертации объекта в строку JSON
     *
     * @param anyObject объект любого типа
     * @param <T>       тип объекта
     * @return json-строку
     */
    public static <T> String converterToJson(T anyObject) {
        return gson.toJson(anyObject);
    }

    /**
     * Метод для конвертации из json-строки обратно в объект
     *
     * @param json      строка в формате json
     * @param classType тип класса, в который будет орагнизовано преобразование
     * @param <T>       тип объекта
     * @return объект
     */
    public static <T> T convertFromJson(String json, Class<T> classType) {
        return gson.fromJson(json, classType);
    }

    /**
     * Метод для конвертации из файла с json-строками одного типа в список таких объектов
     *
     * @param fileName  имя файла
     * @param classType тип класса
     * @param <T>       тип класса
     * @return список объектов одного типа
     * @throws IOException
     */
    public static <T> List<T> getListFromJsonFile(String fileName, Class<T> classType) throws IOException {
        List<T> list = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                list.add(convertFromJson(line, classType));
            }
        }
        return list;
    }

    /**
     * Метод для конвертации и записи списка однотипных объектов в файл
     *
     * @param list     список объектов
     * @param fileName имя файла
     * @param <T>      тип класса
     * @throws IOException
     */
    public static <T> void writeListToJsonFile(List<T> list, String fileName) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            for (T t : list) {
                bufferedWriter.write(converterToJson(t) + "\n");
            }
        }
    }

    /**
     * Метод для конвертации единственного объекта (например, на базе внутреннего списка) в файле (или корневого) в объект java
     *
     * @param object   объект для конвертации
     * @param fileName имя файла
     * @param <T>      тип
     * @throws IOException
     */
    public static <T> void writeToJsonFile(T object, String fileName) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName))) {
            bufferedWriter.write(converterToJson(object) + "\n");
        }
    }

    /**
     * Метод для конвертации единственного объекта (например, на базе внутреннего списка) в файле (или корневого) в объект java
     * В случае параметризованного класса - на выходе работать с сырым типом
     *
     * @param fileName  имя файла
     * @param classType тип класса
     * @param <T>       тип
     * @return объект
     * @throws IOException
     */
    public static <T> T getFromJsonFile(String fileName, Class<T> classType) throws IOException {
        String jsonContent = Files.readString(Paths.get(fileName));
        return convertFromJson(jsonContent, classType);
    }

    /**
     * Метод преобразование json-строки в Json-объект
     *
     * @param string json-строка
     * @return Json-объект
     */
    public static JsonObject getJsonObjectFromString(String string) {
        JsonParser parser = new JsonParser();
        return parser.parse(string).getAsJsonObject();
    }
}



