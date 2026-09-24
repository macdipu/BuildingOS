import 'package:customer/core/usecases/usecase.dart';
import 'package:customer/features/building_application/domain/entities/application_document.dart';
import 'package:customer/features/building_application/domain/entities/application_draft.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/building_application/domain/entities/building_application.dart';
import 'package:customer/features/building_application/domain/entities/draft_field.dart';
import 'package:customer/features/building_application/domain/repositories/building_application_repository.dart';
import 'package:customer/features/building_application/domain/usecases/create_application_use_case.dart';
import 'package:customer/features/building_application/domain/usecases/update_application_use_case.dart';
import 'package:customer/features/building_application/presentation/controllers/application_form_controller.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

class _FakeRepository implements BuildingApplicationRepository {
  final created = <ApplicationDraft>[];
  final updated = <(String, ApplicationDraft)>[];

  BuildingApplication _saved(ApplicationDraft draft) => BuildingApplication(
      id: 'app-1',
      applicationNumber: 'BA-2026-000001',
      status: ApplicationStatus.draft,
      details: draft);

  @override
  ResultFuture<BuildingApplication> create(ApplicationDraft draft) async {
    created.add(draft);
    return Right(_saved(draft));
  }

  @override
  ResultFuture<BuildingApplication> update(
      String id, ApplicationDraft draft) async {
    updated.add((id, draft));
    return Right(_saved(draft));
  }

  @override
  noSuchMethod(Invocation invocation) => throw UnimplementedError();
}

void main() {
  late _FakeRepository repository;
  late ApplicationFormController controller;

  setUp(() {
    Get.testMode = true;
    repository = _FakeRepository();
    controller = ApplicationFormController(
      create: CreateApplicationUseCase(repository),
      update: UpdateApplicationUseCase(repository),
    );
  });

  tearDown(Get.reset);

  test('invalid formats block saving and are reported per field', () async {
    controller.texts[DraftField.contactPhone]!.text = '12345';
    controller.texts[DraftField.estimatedUnits]!.text = '0';

    await controller.save();

    expect(repository.created, isEmpty);
    expect(controller.errors.keys,
        containsAll([DraftField.contactPhone, DraftField.estimatedUnits]));
    expect(controller.errorFor(DraftField.contactPhone), isNotNull);
  });

  Future<void> app(WidgetTester tester) =>
      tester.pumpWidget(const GetMaterialApp(home: SizedBox()));

  testWidgets(
      'a partial draft saves; the relationship note is only sent for OTHER',
      (tester) async {
    await app(tester);
    controller.texts[DraftField.buildingName]!.text = 'Rose Garden';
    controller.texts[DraftField.estimatedUnits]!.text = '12';
    controller.texts[DraftField.relationshipNote]!.text = 'ignored';
    controller.relationship.value = ApplicantRelationship.owner;

    await controller.save();
    await tester.pumpAndSettle(const Duration(seconds: 5));

    final draft = repository.created.single;
    expect(draft.buildingName, 'Rose Garden');
    expect(draft.estimatedUnits, 12);
    expect(draft.relationshipNote, isNull);
    expect(controller.errors, isEmpty);
  });

  testWidgets('editing prefills fields and updates instead of creating',
      (tester) async {
    await app(tester);
    controller.prefill(const BuildingApplication(
      id: 'app-9',
      applicationNumber: 'BA-2026-000009',
      status: ApplicationStatus.moreInformationRequired,
      details: ApplicationDraft(
          buildingName: 'Lake View',
          totalFloors: 6,
          latitude: 23.1,
          longitude: 90.2),
    ));

    expect(controller.isEditing, isTrue);
    expect(controller.texts[DraftField.buildingName]!.text, 'Lake View');
    expect(controller.longitude.text, '90.2');

    await controller.save();
    await tester.pumpAndSettle(const Duration(seconds: 5));

    expect(repository.created, isEmpty);
    expect(repository.updated.single.$1, 'app-9');
    expect(repository.updated.single.$2.totalFloors, 6);
    expect(repository.updated.single.$2.latitude, 23.1);
  });

  test('document entity is plain data', () {
    const document = ApplicationDocument(
        id: 'd',
        fileName: 'deed.pdf',
        contentType: 'application/pdf',
        sizeBytes: 1);
    expect(document.fileName, 'deed.pdf');
  });
}
