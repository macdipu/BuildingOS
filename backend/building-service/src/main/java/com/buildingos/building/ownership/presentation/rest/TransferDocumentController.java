package com.buildingos.building.ownership.presentation.rest;

import com.buildingos.building.ownership.application.downloadtransferdocument.DownloadTransferDocumentQuery;
import com.buildingos.building.ownership.application.downloadtransferdocument.DownloadTransferDocumentUseCase;
import com.buildingos.building.ownership.application.listtransferdocuments.ListTransferDocumentsQuery;
import com.buildingos.building.ownership.application.listtransferdocuments.ListTransferDocumentsUseCase;
import com.buildingos.building.ownership.application.removetransferdocument.RemoveTransferDocumentCommand;
import com.buildingos.building.ownership.application.removetransferdocument.RemoveTransferDocumentUseCase;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentCommand;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentUseCase;
import com.buildingos.building.ownership.presentation.rest.request.RemoveTransferDocumentRequest;
import com.buildingos.building.ownership.presentation.rest.response.TransferDocumentResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Transfer documents (UO-13): admins attach/remove; admins and the transfer's parties list and download. */
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/units/{unitId}/ownership-transfers/{transferId}/documents")
public class TransferDocumentController {
    private final UploadTransferDocumentUseCase upload;
    private final ListTransferDocumentsUseCase list;
    private final DownloadTransferDocumentUseCase download;
    private final RemoveTransferDocumentUseCase remove;

    public TransferDocumentController(UploadTransferDocumentUseCase upload, ListTransferDocumentsUseCase list,
            DownloadTransferDocumentUseCase download, RemoveTransferDocumentUseCase remove) {
        this.upload = upload;
        this.list = list;
        this.download = download;
        this.remove = remove;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<TransferDocumentResponse> upload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, @PathVariable UUID transferId,
            @RequestPart("file") MultipartFile file, @RequestPart(name = "reason", required = false) String reason,
            HttpServletRequest request) throws IOException {
        var stored = upload.execute(CurrentActor.from(jwt), new UploadTransferDocumentCommand(buildingId, unitId,
                transferId, file.getOriginalFilename(), file.getBytes(), reason));
        return ApiEnvelope.of(TransferDocumentResponse.of(stored), CorrelationFilter.traceId(request));
    }

    @GetMapping
    public ApiEnvelope<List<TransferDocumentResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, @PathVariable UUID transferId,
            HttpServletRequest request) {
        var documents = list.execute(CurrentActor.from(jwt),
                new ListTransferDocumentsQuery(buildingId, unitId, transferId));
        return ApiEnvelope.of(documents.stream().map(TransferDocumentResponse::of).toList(),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, @PathVariable UUID transferId,
            @PathVariable UUID documentId) {
        var content = download.execute(CurrentActor.from(jwt),
                new DownloadTransferDocumentQuery(buildingId, unitId, transferId, documentId));
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
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId, @PathVariable UUID unitId,
            @PathVariable UUID transferId, @PathVariable UUID documentId,
            @RequestBody RemoveTransferDocumentRequest body) {
        remove.execute(CurrentActor.from(jwt),
                new RemoveTransferDocumentCommand(buildingId, unitId, transferId, documentId, body.reason()));
    }
}
