package org.example.util;

import org.example.model.cbr.CbrResponse;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class XmlConverterUtilTest {

    /**
     * Тест работы метода маршаллинга объекта в XML-формат
     */
    @Test
    void marshall() {
        CbrResponse cbrResponse = new CbrResponse();
        assertDoesNotThrow(() -> {
            Writer writer = XmlConverterUtil.marshall(cbrResponse);
        }, String.valueOf(JAXBException.class));
    }

    /**
     * Тест работы метода демаршаллинга
     * @throws IOException
     */
    @Test
    void unmarshall() throws IOException {
        URL url = new URL("https://cbr.ru/scripts/XML_daily.asp");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try (InputStreamReader inputStreamReader = new InputStreamReader(con.getInputStream())){
            assertDoesNotThrow(() -> {
                CbrResponse response = XmlConverterUtil.unmarshall(inputStreamReader, CbrResponse.class);
            }, String.valueOf(JAXBException.class));
        }
    }
}