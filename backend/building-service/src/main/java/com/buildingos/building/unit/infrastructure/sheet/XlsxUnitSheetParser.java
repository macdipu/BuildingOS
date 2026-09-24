package com.buildingos.building.unit.infrastructure.sheet;

import com.buildingos.building.shared.domain.model.DomainRuleException;
import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.port.out.UnitSheetParser;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Minimal .xlsx reader (operator decision F4-T3b: no third-party parser). Reads only the first worksheet and its
 * shared strings as data. Refuses formula cells, macro content, DTDs/external entities and archives that exceed the
 * entry-count or decompression limits, so a small upload cannot expand into a large one.
 */
@Component
public class XlsxUnitSheetParser implements UnitSheetParser {
    static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_ENTRIES = 64;
    private static final long MAX_EXPANDED_BYTES = 16L * 1024 * 1024;
    private static final int MAX_EXPANSION_RATIO = 100;
    private static final int MAX_COLUMNS = 64;
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx") || CONTENT_TYPE.equalsIgnoreCase(contentType);
    }

    @Override
    public List<BatchRowInput> parse(byte[] content, int maxRows) {
        var parts = unzip(content);
        String sheetPath = firstSheetPath(parts);
        List<String> shared = parts.containsKey("xl/sharedStrings.xml")
                ? sharedStrings(parts.get("xl/sharedStrings.xml")) : List.of();
        byte[] sheet = parts.get(sheetPath);
        if (sheet == null) {
            throw invalid("The workbook has no readable worksheet");
        }
        return SheetTable.rows(records(sheet, shared, maxRows + 1), maxRows);
    }

    private static Map<String, byte[]> unzip(byte[] content) {
        long budget = Math.min(MAX_EXPANDED_BYTES, (long) content.length * MAX_EXPANSION_RATIO);
        Map<String, byte[]> parts = new HashMap<>();
        int entries = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw tooLarge("The workbook has too many parts");
                }
                String name = entry.getName();
                if (name.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) {
                    throw BatchRejectedException.invalid("SHEET_MACROS_NOT_ALLOWED", "Macro-enabled workbooks are refused");
                }
                boolean wanted = name.equals("xl/workbook.xml") || name.equals("xl/_rels/workbook.xml.rels")
                        || name.equals("xl/sharedStrings.xml") || name.startsWith("xl/worksheets/");
                byte[] bytes = read(zip, budget);
                budget -= bytes.length;
                if (wanted && !entry.isDirectory()) {
                    parts.put(name, bytes);
                }
            }
        } catch (ZipException | IllegalArgumentException notAZip) {
            throw invalid("The file is not a valid .xlsx workbook");
        } catch (IOException unreadable) {
            throw invalid("The file is not a valid .xlsx workbook");
        }
        if (!parts.containsKey("xl/workbook.xml")) {
            throw invalid("The file is not a valid .xlsx workbook");
        }
        return parts;
    }

    /** Reads one entry, counting every expanded byte (kept or not) against the shared budget. */
    private static byte[] read(InputStream in, long budget) throws IOException {
        var out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) > 0) {
            total += n;
            if (total > budget) {
                throw tooLarge("The workbook expands beyond the allowed size");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String firstSheetPath(Map<String, byte[]> parts) {
        String relationId = null;
        try {
            var xml = reader(parts.get("xl/workbook.xml"));
            while (xml.hasNext() && relationId == null) {
                if (xml.next() == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("sheet")) {
                    relationId = xml.getAttributeValue(REL_NS, "id");
                }
            }
            if (relationId == null || !parts.containsKey("xl/_rels/workbook.xml.rels")) {
                return "xl/worksheets/sheet1.xml";
            }
            var rels = reader(parts.get("xl/_rels/workbook.xml.rels"));
            while (rels.hasNext()) {
                if (rels.next() == XMLStreamConstants.START_ELEMENT && rels.getLocalName().equals("Relationship")
                        && relationId.equals(rels.getAttributeValue(null, "Id"))) {
                    String target = rels.getAttributeValue(null, "Target");
                    return target.startsWith("/") ? target.substring(1) : "xl/" + target;
                }
            }
        } catch (XMLStreamException malformed) {
            throw invalid("The workbook XML is malformed");
        }
        return "xl/worksheets/sheet1.xml";
    }

    private static List<String> sharedStrings(byte[] part) {
        List<String> strings = new ArrayList<>();
        try {
            var xml = reader(part);
            StringBuilder current = null;
            boolean phonetic = false;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "si" -> current = new StringBuilder();
                        case "rPh" -> phonetic = true;
                        case "t" -> {
                            String text = xml.getElementText();
                            if (current != null && !phonetic) {
                                current.append(text);
                            }
                        }
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (xml.getLocalName().equals("rPh")) {
                        phonetic = false;
                    } else if (xml.getLocalName().equals("si") && current != null) {
                        strings.add(current.toString());
                        current = null;
                    }
                }
            }
        } catch (XMLStreamException malformed) {
            throw invalid("The shared strings XML is malformed");
        }
        return strings;
    }

    private static List<SheetTable.Record> records(byte[] sheet, List<String> shared, int limit) {
        List<SheetTable.Record> records = new ArrayList<>();
        try {
            var xml = reader(sheet);
            List<String> cells = null;
            int rowNumber = 0;
            int headerRow = -1;
            int column = -1;
            String type = null;
            String value = null;
            while (xml.hasNext()) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "row" -> {
                            String r = xml.getAttributeValue(null, "r");
                            rowNumber = r == null ? rowNumber + 1 : Integer.parseInt(r);
                            cells = new ArrayList<>();
                        }
                        case "c" -> {
                            column = columnIndex(xml.getAttributeValue(null, "r"), cells);
                            type = xml.getAttributeValue(null, "t");
                            value = null;
                        }
                        case "f" -> throw SheetTable.formula(headerRow < 0 ? 0 : rowNumber - headerRow,
                                "cell " + (column + 1));
                        case "v" -> value = xml.getElementText();
                        case "t" -> value = (value == null ? "" : value) + xml.getElementText();
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && cells != null) {
                    if (xml.getLocalName().equals("c") && column >= 0 && column < MAX_COLUMNS) {
                        while (cells.size() <= column) {
                            cells.add(null);
                        }
                        cells.set(column, text(type, value, shared));
                    } else if (xml.getLocalName().equals("row")) {
                        if (headerRow < 0) {
                            headerRow = rowNumber;
                        }
                        records.add(new SheetTable.Record(rowNumber - headerRow, cells));
                        cells = null;
                        if (records.size() > limit) {
                            throw SheetTable.tooLarge(limit - 1);
                        }
                    }
                }
            }
        } catch (XMLStreamException | NumberFormatException malformed) {
            throw invalid("The worksheet XML is malformed");
        }
        return records;
    }

    private static int columnIndex(String reference, List<String> cells) {
        if (reference == null) {
            return cells == null ? 0 : cells.size();
        }
        int index = 0;
        for (char c : reference.toCharArray()) {
            if (c < 'A' || c > 'Z') {
                break;
            }
            index = index * 26 + (c - 'A' + 1);
            if (index > MAX_COLUMNS) {
                return MAX_COLUMNS;
            }
        }
        return index - 1;
    }

    private static String text(String type, String value, List<String> shared) {
        if (value == null) {
            return null;
        }
        if ("s".equals(type)) {
            int index = Integer.parseInt(value.strip());
            if (index < 0 || index >= shared.size()) {
                throw invalid("The worksheet references a missing shared string");
            }
            return shared.get(index);
        }
        if ("e".equals(type)) {
            throw invalid("The worksheet contains an error cell");
        }
        return value;
    }

    /** DTDs and external entities are disabled; a DOCTYPE is treated as a malformed workbook. */
    private static XMLStreamReader reader(byte[] part) throws XMLStreamException {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        var xml = factory.createXMLStreamReader(new ByteArrayInputStream(part));
        return new DtdRejectingReader(xml);
    }

    private static BatchRejectedException invalid(String message) {
        return BatchRejectedException.invalid("SHEET_INVALID", message);
    }

    private static BatchRejectedException tooLarge(String message) {
        return new BatchRejectedException("SHEET_TOO_LARGE", DomainRuleException.Kind.TOO_LARGE, message);
    }
}
