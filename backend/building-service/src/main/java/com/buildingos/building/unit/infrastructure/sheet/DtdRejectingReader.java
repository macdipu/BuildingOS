package com.buildingos.building.unit.infrastructure.sheet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/** Fails on any DOCTYPE or entity reference so workbook XML can never pull in declared or external entities. */
final class DtdRejectingReader extends StreamReaderDelegate {
    DtdRejectingReader(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public int next() throws XMLStreamException {
        int event = super.next();
        if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
            throw new XMLStreamException("DTDs and entity references are not allowed");
        }
        return event;
    }
}
