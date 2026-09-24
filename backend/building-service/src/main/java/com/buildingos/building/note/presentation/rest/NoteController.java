package com.buildingos.building.note.presentation.rest;

import com.buildingos.building.note.application.addnote.AddNoteCommand;
import com.buildingos.building.note.application.addnote.AddNoteUseCase;
import com.buildingos.building.note.application.listnotes.ListNotesQuery;
import com.buildingos.building.note.application.listnotes.ListNotesUseCase;
import com.buildingos.building.note.presentation.rest.request.NoteRequest;
import com.buildingos.building.note.presentation.rest.response.NoteResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/building-applications/{applicationId}/notes")
public class NoteController {
    private final AddNoteUseCase add;
    private final ListNotesUseCase list;

    public NoteController(AddNoteUseCase add, ListNotesUseCase list) {
        this.add = add;
        this.list = list;
    }

    @GetMapping
    public ApiEnvelope<List<NoteResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            HttpServletRequest request) {
        return ApiEnvelope.of(list.execute(CurrentActor.from(jwt), new ListNotesQuery(applicationId)).stream()
                .map(NoteResponse::of).toList(), CorrelationFilter.traceId(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<NoteResponse> add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @RequestBody NoteRequest body, HttpServletRequest request) {
        return ApiEnvelope.of(NoteResponse.of(add.execute(CurrentActor.from(jwt),
                new AddNoteCommand(applicationId, body.body()))), CorrelationFilter.traceId(request));
    }
}
