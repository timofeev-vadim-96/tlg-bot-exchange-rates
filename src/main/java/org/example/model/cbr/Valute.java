package org.example.model.cbr;

import lombok.*;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
public class Valute {
    @XmlAttribute(name = "ID")
    private String id;
    @XmlElement(name = "NumCode")
    private int numCode;
    @XmlElement(name = "CharCode")
    private String charCode;
    @XmlElement(name = "Nominal")
    private int nominal;
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Value")
    private String value;
    @XmlElement(name = "VunitRate")
    private String vUnitRate; //отношение двух валют
}
