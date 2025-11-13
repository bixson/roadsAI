package dk.ek.roadsai.dto.yr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// YR.no forecast JSON to DTO converter
public class YrNoForecastDto {
    @JsonProperty("type") public String type;
    @JsonProperty("geometry") public Object geometry;
    @JsonProperty("properties") public Properties properties;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        @JsonProperty("timeseries") public List<TimeStep> timeseries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeStep {
        @JsonProperty("time") public String time;
        @JsonProperty("data") public Data data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("instant") public InstantDetails instant;
        @JsonProperty("next_1_hours") public NextHours next1Hours;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstantDetails {
        @JsonProperty("details") public Details details;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        @JsonProperty("air_temperature") public Double airTemperature;
        @JsonProperty("wind_speed") public Double windSpeed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NextHours {
        @JsonProperty("details") public PrecipitationDetails details;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrecipitationDetails {
        @JsonProperty("precipitation_amount") public Double precipitationAmount;
    }
}

