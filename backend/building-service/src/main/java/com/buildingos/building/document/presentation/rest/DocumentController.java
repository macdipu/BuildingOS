package com.buildingos.building.document.presentation.rest;

import com.buildingos.building.document.application.downloaddocument.DownloadDocumentQuery;
import com.buildingos.building.document.application.downloaddocument.DownloadDocumentUseCase;
import com.buildingos.building.document.application.listdocuments.ListDocumentsQuery;
import com.buildingos.building.document.application.listdocuments.ListDocumentsUseCase;
import com.buildingos.building.document.application.removedocument.RemoveDocumentCommand;
import com.buildingos.building.document.application.removedocument.RemoveDocumentUseCase;
import com.buildingos.building.document.application.uploaddocument.UploadDocumentCommand;
import com.buildingos.building.document.application.uploaddocument.UploadDocumentUseCase;
import com.buildingos.building.document.presentation.rest.response.DocumentResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Verification documents: applicant uploads/removes while editable; applicant and platform admins read. */
@RestController
@RequestMapping("/api/v1/building-applications/{applicationId}/documents")
public class DocumentController {
    private final UploadDocumentUseCase upload;
    private final RemoveDocumentUseCase remove;
    private final DownloadDocumentUseCase download;
    private final ListDocumentsUseCase list;

    public DocumentController(UploadDocumentUseCase upload, RemoveDocumentUseCase remove,
            DownloadDocumentUseCase download, ListDocumentsUseCase list) {
        this.upload = upload;
        this.remove = remove;
        this.download = download;
        this.list = list;
    }

    @GetMapping
    public ApiEnvelope<List<DocumentResponse>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            HttpServletRequest request) {
        return ApiEnvelope.of(list.execute(CurrentActor.from(jwt), new ListDocumentsQuery(applicationId)).stream()
                .map(DocumentResponse::of).toList(), CorrelationFilter.traceId(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<DocumentResponse> upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @RequestPart("file") MultipartFile file, HttpServletRequest request) throws IOException {
        var stored = upload.execute(CurrentActor.from(jwt),
                new UploadDocumentCommand(applicationId, file.getOriginalFilename(), file.getBytes()));
        return ApiEnvelope.of(DocumentResponse.of(stored), CorrelationFilter.traceId(request));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @PathVariable UUID documentId) {
        var content = download.execute(CurrentActor.from(jwt), new DownloadDocumentQuery(applicationId, documentId));
        var document = content.document();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.type().contentType()))
                .contentLength(document.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName(), StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(content.content()));
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @PathVariable UUID documentId) {
        remove.execute(CurrentActor.from(jwt), new RemoveDocumentCommand(applicationId, documentId));
    }
}
