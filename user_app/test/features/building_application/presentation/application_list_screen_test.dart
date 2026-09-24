import 'package:customer/core/usecases/usecase.dart';
import 'package:customer/features/building_application/domain/entities/application_draft.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/building_application/domain/entities/building_application.dart';
import 'package:customer/features/building_application/domain/repositories/building_application_repository.dart';
import 'package:customer/features/building_application/domain/usecases/list_my_applications_use_case.dart';
import 'package:customer/features/building_application/presentation/controllers/application_list_controller.dart';
import 'package:customer/features/building_application/presentation/pages/application_list_screen.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:customer/core/errors/failure.dart';
import 'package:dartz/dartz.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

class _Repository implements BuildingApplicationRepository {
  _Repository(this.result);

  final Either<Failure, List<BuildingApplication>> result;

  @override
  ResultFuture<List<BuildingApplication>> listMine() async => result;

  @override
  noSuchMethod(Invocation invocation) => throw UnimplementedError();
}

Future<void> _pump(WidgetTester tester,
    Either<Failure, List<BuildingApplication>> result, Locale locale) async {
  Get.testMode = true;
  Get.lazyPut(() => ApplicationListController(
      listMine: ListMyApplicationsUseCase(_Repository(result))));
  await tester.pumpWidget(GetMaterialApp(
    translations: AppTranslations(),
    locale: locale,
    home: const ApplicationListScreen(),
  ));
  await tester.pumpAndSettle();
}

void main() {
  tearDown(Get.reset);

  testWidgets('shows applications with number and localized status',
      (tester) async {
    await _pump(
      tester,
      const Right([
        BuildingApplication(
          id: 'a',
          applicationNumber: 'BA-2026-000001',
          status: ApplicationStatus.moreInformationRequired,
          details: ApplicationDraft(buildingName: 'Rose Garden'),
        ),
      ]),
      const Locale('en'),
    );

    expect(find.text('Rose Garden'), findsOneWidget);
    expect(find.text('Application no. BA-2026-000001'), findsOneWidget);
    expect(find.text('More information needed'), findsOneWidget);
    expect(find.text('Register a building'), findsOneWidget);
  });

  testWidgets('shows the Bangla empty state', (tester) async {
    await _pump(tester, const Right([]), const Locale('bn'));

    expect(find.text('আপনার এখনও কোনো ভবন আবেদন নেই।'), findsOneWidget);
    expect(find.text('ভবন নিবন্ধন করুন'), findsOneWidget);
  });

  testWidgets('a connection failure shows a localized retry', (tester) async {
    await _pump(tester, const Left(ConnectionFailure('CONNECTION')),
        const Locale('en'));

    expect(find.text('Could not connect. Check your internet and try again.'),
        findsOneWidget);
    expect(find.text('Retry'), findsOneWidget);
  });
}
