package org.example.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.stream.Collectors;

/**
 * Класс для конвертации объектов в XML-формат и обратно
 */
public class XmlConverterUtil {

    /**
     * Метод для маршалинга подготовленых объектов с помощью библиотеки JAXB в файл XML
     *
     * @param convertableObject объект для маршалинга
     * @throws JAXBException
     * @throws IOException
     */
    public static <T> Writer marshall(T convertableObject) throws JAXBException, IOException {
        try (StringWriter writer = new StringWriter()) {
            JAXBContext context = JAXBContext.newInstance(convertableObject.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(convertableObject, writer);
            return writer;
        }
    }

    /**
     * Метод для анмаршаллинга объекта из потока чтения
     * @param in объект типа Reader
     * @param classType класс, к которому кастуем
     * @return T t
     * @param <T> тип
     */
    public static <T> T unmarshall(Reader in, Class<T> classType) throws IOException, JAXBException {
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

