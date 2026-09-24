package com.buildingos.building.document.infrastructure.persistence;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.model.DocumentType;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcApplicationDocumentRepositoryAdapter implements ApplicationDocumentRepository {
    private static final String COLUMNS =
            "id, application_id, object_key, file_name, content_type, size_bytes, uploaded_by, uploaded_at";
    private final JdbcTemplate jdbc;

    public JdbcApplicationDocumentRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(ApplicationDocument d) {
        jdbc.update("INSERT INTO application_document (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                d.id(), d.applicationId(), d.objectKey(), d.fileName(), d.type().contentType(), d.sizeBytes(),
                d.uploadedBy(), Timestamp.from(d.uploadedAt()));
    }

    @Override
    public void delete(UUID documentId) {
        jdbc.update("DELETE FROM application_document WHERE id = ?", documentId);
    }

    @Override
    public Optional<ApplicationDocument> find(UUID applicationId, UUID documentId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM application_document WHERE application_id = ? AND id = ?",
                this::map, applicationId, documentId).stream().findFirst();
    }

    @Override
    public List<ApplicationDocument> findByApplication(UUID applicationId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM application_document WHERE application_id = ? "
                + "ORDER BY uploaded_at, id", this::map, applicationId);
    }

    @Override
    public int countByApplication(UUID applicationId) {
        return jdbc.queryForObject("SELECT count(*) FROM application_document WHERE application_id = ?",
                Integer.class, applicationId);
    }

    private ApplicationDocument map(ResultSet rs, int row) throws SQLException {
        return new ApplicationDocument(rs.getObject("id", UUID.class), rs.getObject("application_id", UUID.class),
                rs.getString("object_key"), rs.getString("file_name"),
                DocumentType.fromContentType(rs.getString("content_type")), rs.getLong("size_bytes"),
                rs.getObject("uploaded_by", UUID.class), rs.getTimestamp("uploaded_at").toInstant());
    }
}
