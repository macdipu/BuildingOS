import 'package:customer/core/usecases/usecase.dart';
import '../entities/application_document.dart';
import '../entities/application_draft.dart';
import '../entities/building_application.dart';

/// Failures carry an [ApplicationErrorCode] (or API error code) as their message.
abstract class BuildingApplicationRepository {
  ResultFuture<List<BuildingApplication>> listMine();
  ResultFuture<BuildingApplication> get(String id);
  ResultFuture<BuildingApplication> create(ApplicationDraft draft);
  ResultFuture<BuildingApplication> update(String id, ApplicationDraft draft);
  ResultFuture<BuildingApplication> submit(String id);
  ResultFuture<List<ApplicationDocument>> listDocuments(String applicationId);
  ResultFuture<ApplicationDocument> uploadDocument(
      String applicationId, String filePath);
  ResultVoid removeDocument(String applicationId, String documentId);
}
