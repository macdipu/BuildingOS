import '../../data/repositories/ownership_repository_impl.dart';
import '../../domain/repositories/ownership_repository.dart';
import '../../domain/usecases/ownership_use_cases.dart';
import 'package:get/get.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import '../../data/datasources/property_remote_datasource.dart';
import '../../data/repositories/property_repository_impl.dart';
import '../../domain/repositories/property_repository.dart';
import '../../domain/usecases/property_use_cases.dart';

class PropertyBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<PropertyRemoteDataSource>(
      () => PropertyRemoteDataSource(
        Get.find<ApiClient>(),
        Get.find<ApiUrl>().baseUrl,
      ),
      fenix: true,
    );
    Get.lazyPut<PropertyRepository>(
      () => PropertyRepositoryImpl(Get.find()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListMyBuildings(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListMyProperties(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListMyInvitations(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ClaimOwnerInvitation(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => GetBuildingAccess(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListBuildingFloors(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => CreateBuildingFloor(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => UpdateBuildingFloor(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListBuildingUnits(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => GetPropertyUnit(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => CreatePropertyUnit(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => UpdatePropertyUnit(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListBuildingMembers(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListBuildingInvitations(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => InviteBuildingOwner(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => RevokeOwnerInvitation(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => RevokeOwnerMembership(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => PreviewUnitRows(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => GenerateUnitPreview(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ImportUnitPreview(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => CommitUnitBatch(Get.find<PropertyRepository>()),
      fenix: true,
    );
    Get.lazyPut<OwnershipRepository>(
      () => OwnershipRepositoryImpl(Get.find()),
      fenix: true,
    );
    Get.lazyPut(
      () => GetCurrentOwnership(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => GetOwnershipHistory(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => AssignUnitOwnership(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => TransferUnitOwnership(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => ListTransferFiles(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => UploadTransferFile(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => DownloadTransferFile(Get.find<OwnershipRepository>()),
      fenix: true,
    );
    Get.lazyPut(
      () => RemoveTransferFile(Get.find<OwnershipRepository>()),
      fenix: true,
    );
  }
}
