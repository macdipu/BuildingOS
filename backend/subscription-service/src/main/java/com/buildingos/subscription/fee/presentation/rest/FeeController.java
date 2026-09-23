package com.buildingos.subscription.fee.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.fee.application.getfeeschedule.GetFeeScheduleQuery;
import com.buildingos.subscription.fee.application.getfeeschedule.GetFeeScheduleUseCase;
import com.buildingos.subscription.fee.application.getfeestatus.GetFeeStatusQuery;
import com.buildingos.subscription.fee.application.getfeestatus.GetFeeStatusUseCase;
import com.buildingos.subscription.fee.application.recordpayment.RecordPaymentCommand;
import com.buildingos.subscription.fee.application.recordpayment.RecordPaymentUseCase;
import com.buildingos.subscription.fee.application.updatefeeschedule.UpdateFeeScheduleCommand;
import com.buildingos.subscription.fee.application.updatefeeschedule.UpdateFeeScheduleUseCase;
import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.Money;
import com.buildingos.subscription.fee.domain.model.PaymentReference;
import com.buildingos.subscription.fee.domain.model.ReferenceType;
import com.buildingos.subscription.fee.presentation.rest.request.FeeScheduleRequest;
import com.buildingos.subscription.fee.presentation.rest.request.RecordPaymentRequest;
import com.buildingos.subscription.fee.presentation.rest.response.FeeScheduleResponse;
import com.buildingos.subscription.fee.presentation.rest.response.FeeStatusResponse;
import com.buildingos.subscription.fee.presentation.rest.response.PaymentResponse;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import com.buildingos.subscription.shared.presentation.rest.Enums;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** One-time fees (D-24): schedule, manual payment records, settled status for the approval check (D-26). */
@RestController
@RequestMapping("/api/v1/platform/fees/{feeCode}")
public class FeeController {
    private final GetFeeScheduleUseCase getSchedule;
    private final UpdateFeeScheduleUseCase updateSchedule;
    private final RecordPaymentUseCase recordPayment;
    private final GetFeeStatusUseCase status;

    public FeeController(GetFeeScheduleUseCase getSchedule, UpdateFeeScheduleUseCase updateSchedule,
            RecordPaymentUseCase recordPayment, GetFeeStatusUseCase status) {
        this.getSchedule = getSchedule;
        this.updateSchedule = updateSchedule;
        this.recordPayment = recordPayment;
        this.status = status;
    }

    @GetMapping
    public ApiEnvelope<FeeScheduleResponse> schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable String feeCode,
            HttpServletRequest request) {
        var schedule = getSchedule.execute(CurrentActor.from(jwt), new GetFeeScheduleQuery(code(feeCode)));
        return ApiEnvelope.of(FeeScheduleResponse.of(schedule), CorrelationFilter.traceId(request));
    }

    @PutMapping
    public ApiEnvelope<FeeScheduleResponse> updateSchedule(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String feeCode, @RequestBody FeeScheduleRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        if (body.required() == null) {
            throw new IllegalArgumentException("required is required");
        }
        var saved = updateSchedule.execute(actor, new UpdateFeeScheduleCommand(code(feeCode),
                new Money(body.amount(), body.currency()), body.required()));
        return ApiEnvelope.of(FeeScheduleResponse.of(saved), CorrelationFilter.traceId(request));
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<PaymentResponse> recordPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable String feeCode,
            @RequestBody RecordPaymentRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        if (body.paidOn() == null) {
            throw new IllegalArgumentException("paidOn is required");
        }
        var payment = recordPayment.execute(actor, new RecordPaymentCommand(code(feeCode),
                reference(body.referenceType(), body.referenceId()), new Money(body.amount(), body.currency()),
                body.externalReference(), body.paidOn()));
        return ApiEnvelope.of(PaymentResponse.of(payment), CorrelationFilter.traceId(request));
    }

    @GetMapping("/status")
    public ApiEnvelope<FeeStatusResponse> status(@AuthenticationPrincipal Jwt jwt, @PathVariable String feeCode,
            @RequestParam String referenceType, @RequestParam UUID referenceId, HttpServletRequest request) {
        var view = status.execute(CurrentActor.from(jwt),
                new GetFeeStatusQuery(code(feeCode), reference(referenceType, referenceId)));
        return ApiEnvelope.of(FeeStatusResponse.of(view), CorrelationFilter.traceId(request));
    }

    private static FeeCode code(String value) {
        return Enums.parse(FeeCode.class, value, "feeCode");
    }

    private static PaymentReference reference(String type, UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("referenceId is required");
        }
        return new PaymentReference(Enums.parse(ReferenceType.class, type, "referenceType"), id);
    }
}
