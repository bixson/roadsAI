package dk.ek.roadsai.dto.vegagerdin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

///  single instance converter XML to DTO
/// sent to VegagerdinArrayDto
public class VegagerdinItemDto {
    @JacksonXmlProperty(localName = "Breidd")     public Double breidd;      // Latitude
    @JacksonXmlProperty(localName = "Lengd")      public Double lengd;       // longitude
    @JacksonXmlProperty(localName = "Dags")       public String dags;        // "4.11.2025 21:50:00"
    @JacksonXmlProperty(localName = "Hiti")       public Double hiti;        // °C
    @JacksonXmlProperty(localName = "Vindhradi")  public Double vindhradi;   // m/s
    @JacksonXmlProperty(localName = "Vindhvida")  public Double vindhvida;   // m/s (gust)
    @JacksonXmlProperty(localName = "Vindatt")    public Integer vindatt;    // wind direction (degrees)
    @JacksonXmlProperty(localName = "VindattAscEng") public String vindattAscEng; // N/E/S/W
    @JacksonXmlProperty(localName = "Raki")       public Double raki;        // humidity (%)
    @JacksonXmlProperty(localName = "Nafn")       public String nafn;        // name
    @JacksonXmlProperty(localName = "Nr")         public Integer nr;         // station number
    @JacksonXmlProperty(localName = "Status")     public String status;
}
