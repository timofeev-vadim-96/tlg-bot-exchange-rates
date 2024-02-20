package org.example.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.stream.Collectors;

/**
 * Класс для конвертации и обратно классов в XML-формат для передачи данных
 */
public class XmlConverterUtil {
    /**
     * Метод для демаршалинга объекта из файла XML в объект Java
     *
     * @param xmlFileName файл XML
     * @return java-объект
     * @throws FileNotFoundException
     * @throws JAXBException
     */
    public static <T> T unmarshall(String xmlFileName, Class<T> classType) throws IOException, JAXBException {
        StringReader reader;
        Unmarshaller unmarshaller;
        try (BufferedReader br = new BufferedReader(new FileReader(xmlFileName))) {
            String body = br.lines().collect(Collectors.joining());
            reader = new StringReader(body);
            JAXBContext context = JAXBContext.newInstance(classType); //в скобках класс, к которому приводим
            unmarshaller = context.createUnmarshaller();
        }
        return (T) unmarshaller.unmarshal(reader);
    }

    /**
     * Метод для маршалинга подготовленых объектов с помощью библиотеки JAXB в файл XML
     *
     * @param convertableObject объект для маршалинга
     * @param xmlFileName       целевой файл для записи XML
     * @throws JAXBException
     * @throws IOException
     */
    public static <T> void marshall(T convertableObject, String xmlFileName) throws JAXBException, IOException {
        try (StringWriter writer = new StringWriter()) {
            File file = new File(xmlFileName);
            JAXBContext context = JAXBContext.newInstance(convertableObject.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(convertableObject, writer);
            marshaller.marshal(convertableObject, file);
        }
    }

    /**
     * Метод для анмаршаллинга объекта из потока чтения
     * @param in объект типа Reader
     * @param classType класс, к которому кастуем
     * @return T t
     * @param <T> тип
     */
    public static <T> T unmarshallStream(Reader in, Class<T> classType) throws IOException, JAXBException {
        StringReader reader;
        Unmarshaller unmarshaller;
        try (BufferedReader br = new BufferedReader(in)) {
            String body = br.lines().collect(Collectors.joining());
            reader = new StringReader(body);
            JAXBContext context = JAXBContext.newInstance(classType); //в скобках класс, к которому приводим
            unmarshaller = context.createUnmarshaller();
        }
        return (T) unmarshaller.unmarshal(reader);
    }
}

