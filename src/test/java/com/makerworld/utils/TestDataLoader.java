package com.makerworld.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestDataLoader {
    private final JsonNode root;

    public TestDataLoader() {
        this.root = loadJson("testdata/search-terms.json");
    }

    private JsonNode loadJson(String resourcePath) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test data resource: " + resourcePath);
            }
            return new ObjectMapper().readTree(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read test data resource: " + resourcePath, e);
        }
    }

    public String commonSearchTerm() {
        return root.path("commonSearchTerm").asText("vase");
    }

    public String secondarySearchTerm() {
        return root.path("secondarySearchTerm").asText("swatch");
    }

    public String rareSearchTerm() {
        return root.path("rareSearchTerm").asText("zzzzmakerworldunlikelyterm");
    }

    public String pinnedModelPath() {
        return root.path("pinnedModelPath").asText("/en/models/544229");
    }

    public List<String> expectedHeaderLinks() {
        List<String> values = new ArrayList<>();
        root.path("expectedHeaderLinks").forEach(node -> values.add(node.asText()));
        return values;
    }

    public List<String> guestLabels() {
        List<String> values = new ArrayList<>();
        root.path("guestLabels").forEach(node -> values.add(node.asText()));
        return values;
    }
}
