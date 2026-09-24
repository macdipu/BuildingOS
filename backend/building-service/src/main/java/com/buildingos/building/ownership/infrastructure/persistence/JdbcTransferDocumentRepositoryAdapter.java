package com.buildingos.building.ownership.infrastructure.persistence;

import com.buildingos.building.document.domain.model.DocumentType;
import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransferDocumentRepositoryAdapter implements TransferDocumentRepository {
    private static final String COLUMNS = "id, building_id, transfer_id, object_key, file_name, content_type, "
            + "size_bytes, uploaded_by, uploaded_at";
    private final JdbcTemplate jdbc;

    public JdbcTransferDocumentRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(TransferDocument d) {
        jdbc.update("INSERT INTO ownership_document (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", d.id(),
                d.buildingId(), d.transferId(), d.objectKey(), d.fileName(), d.type().contentType(), d.sizeBytes(),
                d.uploadedBy(), Timestamp.from(d.uploadedAt()));
    }

    @Override
    public int countActive(UUID buildingId, UUID transferId) {
        return jdbc.queryForObject("SELECT count(*) FROM ownership_document WHERE building_id = ? AND transfer_id = ? "
                + "AND removed_at IS NULL", Integer.class, buildingId, transferId);
    }

    @Override
    public List<TransferDocument> listActive(UUID buildingId, UUID transferId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ownership_document WHERE building_id = ? AND transfer_id = ? "
                + "AND removed_at IS NULL ORDER BY uploaded_at, id", this::document, buildingId, transferId);
    }

    @Override
    public Optional<TransferDocument> findActive(UUID buildingId, UUID transferId, UUID documentId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ownership_document WHERE building_id = ? AND transfer_id = ? "
                        + "AND id = ? AND removed_at IS NULL", this::document, buildingId, transferId, documentId)
                .stream().findFirst();
    }

    @Override
    public void markRemoved(UUID documentId, UUID removedBy, Instant removedAt, String reason) {
        jdbc.update("UPDATE ownership_document SET removed_by = ?, removed_at = ?, removal_reason = ? WHERE id = ?",
                removedBy, Timestamp.from(removedAt), reason, documentId);
    }

    private TransferDocument document(ResultSet rs, int row) throws SQLException {
        return new TransferDocument(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("transfer_id", UUID.class), rs.getString("object_key"), rs.getString("file_name"),
                DocumentType.fromContentType(rs.getString("content_type")), rs.getLong("size_bytes"),
                rs.getObject("uploaded_by", UUID.class), rs.getTimestamp("uploaded_at").toInstant());
    }
}
