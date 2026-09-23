package com.buildingos.subscription.fee.application.recordpayment;

import com.buildingos.subscription.fee.application.FeeErrors;
import com.buildingos.subscription.fee.domain.model.PaymentMethod;
import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import com.buildingos.subscription.fee.domain.repository.PaymentRecordRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.BusinessRuleException;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Admin records an offline payment (D-24); a gateway adapter would add another {@link PaymentMethod}. */
public final class RecordPaymentService implements RecordPaymentUseCase {
    private final FeeScheduleRepository schedules;
    private final PaymentRecordRepository payments;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RecordPaymentService(FeeScheduleRepository schedules, PaymentRecordRepository payments,
            AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.schedules = schedules;
        this.payments = payments;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public PaymentRecord execute(Actor actor, RecordPaymentCommand command) {
        actor.requireRevenueAdmin();
        return unitOfWork.inTransaction(() -> {
            var schedule = schedules.find(command.feeCode()).orElseThrow(() -> FeeErrors.notConfigured(command.feeCode()));
            if (!schedule.price().currency().equals(command.paid().currency())) {
                throw BusinessRuleException.conflict("CURRENCY_MISMATCH",
                        "Fee is charged in " + schedule.price().currency());
            }
            var now = clock.instant();
            var payment = new PaymentRecord(UUID.randomUUID(), command.feeCode(), command.reference(),
                    command.paid(), PaymentMethod.MANUAL, command.externalReference(), command.paidOn(),
                    actor.userId(), now);
            payments.insert(payment);
            var after = new LinkedHashMap<String, Object>();
            after.put("feeCode", payment.feeCode().name());
            after.put("referenceType", payment.reference().type().name());
            after.put("referenceId", payment.reference().id().toString());
            after.put("amount", payment.paid().amount().toPlainString());
            after.put("currency", payment.paid().currency());
            after.put("method", payment.method().name());
            after.put("externalReference", payment.externalReference());
            after.put("paidOn", payment.paidOn().toString());
            audit.record(new AuditRecorder.Entry(actor.userId(), "PAYMENT_RECORDED", "PAYMENT",
                    payment.id().toString(), null, after, now));
            return payment;
        });
    }
}
