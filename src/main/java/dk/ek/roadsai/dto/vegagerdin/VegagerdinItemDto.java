package dk.ek.roadsai.dto.vegagerdin;

import com.fasterxml.jackson.annotation.JsonProperty;

///  single instance converter JSON to DTO
public class VegagerdinItemDto {
    @JsonProperty("Breidd")     public Double breidd;      // Latitude
    @JsonProperty("Lengd")      public Double lengd;       // longitude
    @JsonProperty("Dags")       public String dags;        // "4.11.2025 21:50:00"
    @JsonProperty("Hiti")       public Double hiti;        // °C
    @JsonProperty("Vindhradi")  public Double vindhradi;   // m/s
    @JsonProperty("Vindhvida")  public Double vindhvida;   // m/s (gust)
    @JsonProperty("Nafn")       public String nafn;        // name
    @JsonProperty("Nr_Vedurstofa") public Integer nrVedurstofa; // official station number
}
