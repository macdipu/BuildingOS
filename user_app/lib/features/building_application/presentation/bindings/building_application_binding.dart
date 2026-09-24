import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:get/get.dart';
import '../../data/datasources/building_application_remote_datasource.dart';
import '../../data/repositories/building_application_repository_impl.dart';
import '../../domain/repositories/building_application_repository.dart';
import '../../domain/usecases/create_application_use_case.dart';
import '../../domain/usecases/get_application_use_case.dart';
import '../../domain/usecases/list_documents_use_case.dart';
import '../../domain/usecases/list_my_applications_use_case.dart';
import '../../domain/usecases/remove_document_use_case.dart';
import '../../domain/usecases/submit_application_use_case.dart';
import '../../domain/usecases/update_application_use_case.dart';
import '../../domain/usecases/upload_document_use_case.dart';
import '../controllers/application_detail_controller.dart';
import '../controllers/application_form_controller.dart';
import '../controllers/application_list_controller.dart';

/// Data and use cases shared by every building-application screen.
class BuildingApplicationDataBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<BuildingApplicationRemoteDataSource>(
        () => BuildingApplicationRemoteDataSource(
            Get.find<ApiClient>(), Get.find<ApiUrl>()),
        fenix: true);
    Get.lazyPut<BuildingApplicationRepository>(
        () => BuildingApplicationRepositoryImpl(
            Get.find<BuildingApplicationRemoteDataSource>()),
        fenix: true);
    BuildingApplicationRepository repo() =>
        Get.find<BuildingApplicationRepository>();
    Get.lazyPut(() => ListMyApplicationsUseCase(repo()), fenix: true);
    Get.lazyPut(() => GetApplicationUseCase(repo()), fenix: true);
    Get.lazyPut(() => CreateApplicationUseCase(repo()), fenix: true);
    Get.lazyPut(() => UpdateApplicationUseCase(repo()), fenix: true);
    Get.lazyPut(() => SubmitApplicationUseCase(repo()), fenix: true);
    Get.lazyPut(() => ListDocumentsUseCase(repo()), fenix: true);
    Get.lazyPut(() => UploadDocumentUseCase(repo()), fenix: true);
    Get.lazyPut(() => RemoveDocumentUseCase(repo()), fenix: true);
  }
}

class ApplicationListBinding extends Bindings {
  @override
  void dependencies() {
    BuildingApplicationDataBinding().dependencies();
    Get.lazyPut(() => ApplicationListController(listMine: Get.find()));
  }
}

class ApplicationFormBinding extends Bindings {
  @override
  void dependencies() {
    BuildingApplicationDataBinding().dependencies();
    Get.lazyPut(() =>
        ApplicationFormController(create: Get.find(), update: Get.find()));
  }
}

class ApplicationDetailBinding extends Bindings {
  @override
  void dependencies() {
    BuildingApplicationDataBinding().dependencies();
    Get.lazyPut(() => ApplicationDetailController(
          getApplication: Get.find(),
          listDocuments: Get.find(),
          uploadDocument: Get.find(),
          removeDocument: Get.find(),
          submitApplication: Get.find(),
        ));
  }
}
