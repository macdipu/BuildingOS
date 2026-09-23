package com.buildingos.subscription.fee.application.recordpayment;

import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import com.buildingos.subscription.shared.application.Actor;

public interface RecordPaymentUseCase {
    PaymentRecord execute(Actor actor, RecordPaymentCommand command);
}
