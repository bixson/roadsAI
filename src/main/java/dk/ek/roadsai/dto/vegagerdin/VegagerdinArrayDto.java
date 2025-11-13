package dk.ek.roadsai.dto.vegagerdin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/// Vegagerðin returns JSON array in API
/// wraps @VegagerdinItemDto into an array
public class VegagerdinArrayDto {

    @JsonProperty("Vedur")
    @JacksonXmlProperty(localName = "Vedur")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<VegagerdinItemDto> vedur;

}
