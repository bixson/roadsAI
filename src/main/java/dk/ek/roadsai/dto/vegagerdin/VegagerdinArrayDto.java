package dk.ek.roadsai.dto.vegagerdin;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

///  Vegargerðin returns XML in API
/// wraps @VegagerdinItemDto into an array
public class VegagerdinArrayDto {

    @JacksonXmlProperty(localName = "Vedur")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<VegagerdinItemDto> vedur;

}
