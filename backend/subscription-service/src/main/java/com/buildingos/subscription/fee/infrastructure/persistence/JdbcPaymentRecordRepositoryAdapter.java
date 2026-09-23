package com.buildingos.subscription.fee.infrastructure.persistence;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.Money;
import com.buildingos.subscription.fee.domain.model.PaymentMethod;
import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import com.buildingos.subscription.fee.domain.model.PaymentReference;
import com.buildingos.subscription.fee.domain.model.ReferenceType;
import com.buildingos.subscription.fee.domain.repository.PaymentRecordRepository;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRecordRepositoryAdapter implements PaymentRecordRepository {
    private final JdbcTemplate jdbc;

    public JdbcPaymentRecordRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(PaymentRecord payment) {
        jdbc.update("INSERT INTO payment_record (id, fee_code, reference_type, reference_id, amount, currency, method, "
                        + "external_reference, paid_on, recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                payment.id(), payment.feeCode().name(), payment.reference().type().name(), payment.reference().id(),
                payment.paid().amount(), payment.paid().currency(), payment.method().name(),
                payment.externalReference(), Date.valueOf(payment.paidOn()), payment.recordedBy(),
                Timestamp.from(payment.recordedAt()));
    }

    @Override
    public List<PaymentRecord> find(FeeCode feeCode, PaymentReference reference) {
        return jdbc.query("SELECT id, fee_code, reference_type, reference_id, amount, currency, method, "
                        + "external_reference, paid_on, recorded_by, recorded_at FROM payment_record "
                        + "WHERE fee_code = ? AND reference_type = ? AND reference_id = ? ORDER BY recorded_at",
                (rs, row) -> new PaymentRecord(rs.getObject("id", UUID.class), FeeCode.valueOf(rs.getString("fee_code")),
                        new PaymentReference(ReferenceType.valueOf(rs.getString("reference_type")),
                                rs.getObject("reference_id", UUID.class)),
                        new Money(rs.getBigDecimal("amount"), rs.getString("currency")),
                        PaymentMethod.valueOf(rs.getString("method")), rs.getString("external_reference"),
                        rs.getDate("paid_on").toLocalDate(), rs.getObject("recorded_by", UUID.class),
                        rs.getTimestamp("recorded_at").toInstant()),
                feeCode.name(), reference.type().name(), reference.id());
    }
}
