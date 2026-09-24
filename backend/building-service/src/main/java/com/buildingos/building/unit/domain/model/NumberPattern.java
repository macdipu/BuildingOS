package com.buildingos.building.unit.domain.model;

/**
 * Unit-number template for generated batches: {@code {floor}} = floor display order, {@code {n}} = position,
 * {@code {nn}} = position zero-padded to two digits, {@code {letter}} = A–Z by position.
 */
public record NumberPattern(String template) {
    private static final int MAX_LETTER = 26;

    public NumberPattern {
        if (template == null || template.isBlank()) {
            throw new InvalidUnitInputException("PATTERN_REQUIRED", "numberPattern", "numberPattern is required");
        }
        if (!template.contains("{n}") && !template.contains("{nn}") && !template.contains("{letter}")) {
            throw new InvalidUnitInputException("PATTERN_NEEDS_POSITION", "numberPattern",
                    "numberPattern needs {n}, {nn} or {letter} so numbers differ within a floor");
        }
    }

    /** {@code index} is 0-based within the floor; {@code start} offsets {n}/{nn} for ranges such as 101–120. */
    public String apply(int floorOrder, int index, int start) {
        if (template.contains("{letter}") && index >= MAX_LETTER) {
            throw new InvalidUnitInputException("PATTERN_LETTER_RANGE", "numberPattern",
                    "{letter} supports at most " + MAX_LETTER + " units per floor");
        }
        int position = start + index;
        return template.replace("{floor}", Integer.toString(floorOrder))
                .replace("{nn}", String.format("%02d", position))
                .replace("{n}", Integer.toString(position))
                .replace("{letter}", String.valueOf((char) ('A' + index)));
    }
}
