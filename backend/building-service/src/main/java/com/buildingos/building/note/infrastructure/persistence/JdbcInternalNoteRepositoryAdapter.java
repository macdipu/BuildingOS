package com.buildingos.building.note.infrastructure.persistence;

import com.buildingos.building.note.domain.model.InternalNote;
import com.buildingos.building.note.domain.repository.InternalNoteRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInternalNoteRepositoryAdapter implements InternalNoteRepository {
    private final JdbcTemplate jdbc;

    public JdbcInternalNoteRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(InternalNote note) {
        jdbc.update("INSERT INTO internal_note (id, application_id, author_user_id, body, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                note.id(), note.applicationId(), note.authorUserId(), note.body(), Timestamp.from(note.createdAt()));
    }

    @Override
    public List<InternalNote> findByApplication(UUID applicationId) {
        return jdbc.query("SELECT id, application_id, author_user_id, body, created_at FROM internal_note "
                + "WHERE application_id = ? ORDER BY created_at, id", this::map, applicationId);
    }

    private InternalNote map(ResultSet rs, int row) throws SQLException {
        return new InternalNote(rs.getObject("id", UUID.class), rs.getObject("application_id", UUID.class),
                rs.getObject("author_user_id", UUID.class), rs.getString("body"),
                rs.getTimestamp("created_at").toInstant());
    }
}
