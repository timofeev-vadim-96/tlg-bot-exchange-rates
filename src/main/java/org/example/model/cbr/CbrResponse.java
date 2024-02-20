package org.example.model.cbr;

import lombok.*;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * �������� ��� ����������� ������ �� ����������� ��
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@XmlRootElement(name = "ValCurs")
@XmlAccessorType(XmlAccessType.FIELD)
public class CbrResponse {
    @XmlAttribute(name = "Date")
    private String date;
    @XmlAttribute(name = "name")
    private String name;
    @XmlElement(name="Valute")
    private List<Valute> valutes;
}
