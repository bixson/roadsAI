package dk.ek.roadsai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import dk.ek.roadsai.model.RoadClosure;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Fetches road closure data from the umferdin.is RSS feed.
 * Caches results for 15 minutes to reduce external API calls.
 */
@Service
public class RoadClosureService {

    private static final String RSS_URL = "https://www.umferdin.is/rss";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final WebClient http = WebClient.builder()
            .defaultHeader("User-Agent", "roadsai/2.0")
            .build();
    private final XmlMapper xml = new XmlMapper();

    private List<RoadClosure> cache = List.of();
    private Instant cacheAt = Instant.EPOCH;

    public List<RoadClosure> fetchClosures() {
        if (Duration.between(cacheAt, Instant.now()).compareTo(TTL) < 0) {
            return cache;
        }
        try {
            String body = http.get().uri(RSS_URL)
                    .retrieve().bodyToMono(String.class).block();
            if (body == null || body.isBlank()) return cache;

            RssDto rss = xml.readValue(body, RssDto.class);
            if (rss == null || rss.channel == null || rss.channel.items == null) return cache;

            cache = rss.channel.items.stream()
                    .filter(item -> item.title != null && !item.title.isBlank())
                    .map(item -> new RoadClosure(
                            item.title.trim(),
                            item.description != null ? item.description.trim() : "",
                            item.pubDate != null ? item.pubDate.trim() : ""))
                    .toList();
            cacheAt = Instant.now();
        } catch (Exception e) {
            // Keep stale cache or empty list on any error
        }
        return cache;
    }

    // --- RSS XML DTOs ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RssDto {
        @JsonProperty("channel")
        public ChannelDto channel;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChannelDto {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("item")
        public List<ItemDto> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ItemDto {
        @JsonProperty("title")
        public String title;
        @JsonProperty("description")
        public String description;
        @JsonProperty("pubDate")
        public String pubDate;
    }
}
