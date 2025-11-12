package dk.ek.roadsai.dto.vegagerdin;

import com.fasterxml.jackson.annotation.JsonProperty;

///  single instance converter JSON to DTO
@SuppressWarnings("unused") // Fields are used via Jackson deserialization
public class VegagerdinItemDto {
    @JsonProperty("Breidd")     public Double breidd;      // Latitude
    @JsonProperty("Lengd")      public Double lengd;       // longitude
    @JsonProperty("Dags")       public String dags;        // "4.11.2025 21:50:00"
    @JsonProperty("Hiti")       public Double hiti;        // °C
    @JsonProperty("Vindhradi")  public Double vindhradi;   // m/s
    @JsonProperty("Vindhvida")  public Double vindhvida;   // m/s (gust)
    @JsonProperty("Vindatt")    public Integer vindatt;    // wind direction (degrees)
    @JsonProperty("VindattAscEng") public String vindattAscEng; // N/E/S/W
    @JsonProperty("Raki")       public Double raki;        // humidity (%)
    @JsonProperty("Nafn")       public String nafn;        // name
    @JsonProperty("Nr")         public Integer nr;         // internal station number
    @JsonProperty("Nr_Vedurstofa") public Integer nrVedurstofa; // official station number
    @JsonProperty("Status")     public String status;
}
