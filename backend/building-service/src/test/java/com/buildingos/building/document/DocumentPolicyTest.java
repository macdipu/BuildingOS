package com.buildingos.building.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.document.domain.model.DocumentRejectedException;
import com.buildingos.building.document.domain.model.DocumentType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentPolicyTest {
    static final byte[] PDF = "%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};
    private final DocumentPolicy policy = new DocumentPolicy(64, 2);

    @Test
    void recognisesTypesByContent() {
        assertThat(policy.accept(PDF)).isEqualTo(DocumentType.PDF);
        assertThat(policy.accept(PNG)).isEqualTo(DocumentType.PNG);
        assertThat(policy.accept(JPEG)).isEqualTo(DocumentType.JPEG);
        assertThatThrownBy(() -> policy.accept("<html>".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(DocumentRejectedException.UnsupportedType.class);
        assertThatThrownBy(() -> policy.accept(new byte[] {0x25, 0x50}))
                .isInstanceOf(DocumentRejectedException.UnsupportedType.class);
    }

    @Test
    void enforcesSizeAndCount() {
        assertThatThrownBy(() -> policy.accept(new byte[0])).hasMessageContaining("empty");
        byte[] big = new byte[65];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        assertThatThrownBy(() -> policy.accept(big)).isInstanceOf(DocumentRejectedException.TooLarge.class);
        policy.requireRoomFor(1);
        assertThatThrownBy(() -> policy.requireRoomFor(2)).isInstanceOf(DocumentRejectedException.LimitReached.class);
    }

    @Test
    void fileNameIsDisplayOnlyAndSanitised() {
        UUID app = UUID.randomUUID();
        var doc = ApplicationDocument.create(app, "../../etc/pa\"ss\u0007wd", DocumentType.PDF, 10, UUID.randomUUID(),
                Instant.now());
        assertThat(doc.fileName()).isEqualTo("passwd");
        assertThat(doc.objectKey()).isEqualTo("applications/" + app + "/" + doc.id());
        assertThat(ApplicationDocument.create(app, "C:\\scan\\deed.pdf", DocumentType.PDF, 1, UUID.randomUUID(),
                Instant.now()).fileName()).isEqualTo("deed.pdf");
        assertThat(ApplicationDocument.create(app, "  ", DocumentType.PDF, 1, UUID.randomUUID(), Instant.now())
                .fileName()).isEqualTo("document");
    }
}
