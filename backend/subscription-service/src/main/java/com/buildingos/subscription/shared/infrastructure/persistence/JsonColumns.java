package com.buildingos.subscription.shared.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Reads/writes jsonb columns. */
@Component
public class JsonColumns {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST = new TypeReference<>() {};
    private final ObjectMapper mapper;

    public JsonColumns(ObjectMapper mapper) { this.mapper = mapper; }

    public String write(Object value) { return value == null ? null : mapper.writeValueAsString(value); }

    public Map<String, Object> readMap(String json) { return json == null ? null : mapper.readValue(json, MAP); }

    public List<String> readList(String json) { return mapper.readValue(json, LIST); }
}
