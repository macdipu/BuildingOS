package com.buildingos.building.document.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public abstract sealed class DocumentRejectedException extends DomainRuleException {
    private DocumentRejectedException(String code, Kind kind, String message) {
        super(code, kind, message);
    }

    public static final class TooLarge extends DocumentRejectedException {
        public TooLarge(long maxBytes) {
            super("DOCUMENT_TOO_LARGE", Kind.TOO_LARGE, "Document exceeds " + maxBytes + " bytes");
        }
    }

    public static final class UnsupportedType extends DocumentRejectedException {
        public UnsupportedType() {
            super("UNSUPPORTED_DOCUMENT_TYPE", Kind.UNSUPPORTED_TYPE, "Only PDF, JPEG and PNG documents are accepted");
        }
    }

    public static final class LimitReached extends DocumentRejectedException {
        public LimitReached(int max) {
            super("DOCUMENT_LIMIT_REACHED", Kind.CONFLICT, "An application can hold at most " + max + " documents");
        }
    }
}
