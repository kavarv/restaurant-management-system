package com.restaurant.rms.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Compares two JSON snapshots (old and new state of an entity) and returns a
 * field-level diff map suitable for side-by-side rendering in a Thymeleaf template.
 *
 * <h3>Return format</h3>
 * <pre>
 * Map&lt;String, Object[]&gt; where each entry is:
 *   key   = field name (e.g. "price")
 *   value = Object[2]{ oldValue, newValue }
 *            — if only one snapshot is provided the other slot is null
 *            — fields identical in both snapshots are excluded
 * </pre>
 *
 * <h3>Thymeleaf rendering</h3>
 * <pre>
 * &lt;tr th:each="entry : ${diff}"&gt;
 *   &lt;td th:text="${entry.key}"&gt;&lt;/td&gt;
 *   &lt;td th:text="${entry.value[0]}" class="old-value"&gt;&lt;/td&gt;
 *   &lt;td th:text="${entry.value[1]}" class="new-value"&gt;&lt;/td&gt;
 * &lt;/tr&gt;
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditDiffUtil {

    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    /**
     * Computes the changed-field diff between two JSON snapshots.
     *
     * @param oldJson JSON string of the entity before mutation (may be null for CREATE)
     * @param newJson JSON string of the entity after  mutation (may be null for DELETE)
     * @return ordered map of changed fields: fieldName → [oldValue, newValue]
     */
    public Map<String, Object[]> compareJsonValues(String oldJson, String newJson) {
        Map<String, Object> oldMap = parseJson(oldJson);
        Map<String, Object> newMap = parseJson(newJson);

        // Collect every key from both snapshots
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        Map<String, Object[]> diff = new LinkedHashMap<>();

        for (String key : allKeys) {
            Object oldVal = oldMap.get(key);
            Object newVal = newMap.get(key);

            // Skip if both are absent or identical
            if (Objects.equals(oldVal, newVal)) continue;

            diff.put(key, new Object[]{ oldVal, newVal });
        }

        return diff;
    }

    /**
     * Same as {@link #compareJsonValues} but returns all fields regardless of
     * whether they changed — useful for CREATE/DELETE where one side is empty.
     */
    public Map<String, Object[]> allFieldsWithDiff(String oldJson, String newJson) {
        Map<String, Object> oldMap = parseJson(oldJson);
        Map<String, Object> newMap = parseJson(newJson);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        Map<String, Object[]> result = new LinkedHashMap<>();
        for (String key : allKeys) {
            result.put(key, new Object[]{ oldMap.get(key), newMap.get(key) });
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("AuditDiffUtil: cannot parse JSON snapshot — returning raw string: {}",
                    ex.getMessage());
            // Wrap the raw 