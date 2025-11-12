package dk.ek.roadsai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ek.roadsai.dto.vedur.is.VedurAwsDto.Aws10minBasic;
import dk.ek.roadsai.dto.vedur.is.VedurAwsDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VedurAwsDtoTest {

    @Test
    void parseBareArrayAndMap() throws Exception {
        String sample = """
        [
          {
            "station_id": "1475",
            "time": "2025-11-05T20:40:00",
            "t": 3.2,
            "f": 6.8,
            "fg": 9.7,
            "vis": 12000
          }
        ]
        """;

        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        List<Aws10minBasic> list = om.readValue(sample, new TypeReference<>() {});
        var obs = VedurAwsDto.map("imo:1475", list);

        assertEquals(1, obs.size());
        assertEquals(3.2, obs.getFirst().tempC());
        assertEquals(6.8, obs.getFirst().windMs());
        assertEquals(9.7, obs.getFirst().gustMs());
    }
}
