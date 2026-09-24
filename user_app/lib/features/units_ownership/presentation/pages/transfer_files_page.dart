import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:file_picker/file_picker.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/entities/ownership_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../../domain/usecases/ownership_use_cases.dart';
import '../widgets/property_widgets.dart';

class TransferFilesPage extends StatefulWidget {
  const TransferFilesPage({
    super.key,
    required this.buildingId,
    required this.unitId,
    required this.transferId,
  });
  final String buildingId, unitId, transferId;
  @override
  State<TransferFilesPage> createState() => _TransferFilesPageState();
}

class _TransferFilesPageState extends State<TransferFilesPage> {
  final reason = TextEditingController();
  @override
  void dispose() {
    reason.dispose();
    super.dispose();
  }

  Future<(BuildingAccess, List<TransferFile>)> load() async => (
    await Get.find<GetBuildingAccess>()(widget.buildingId),
    await Get.find<ListTransferFiles>()(
      widget.buildingId,
      widget.unitId,
      widget.transferId,
    ),
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoDocuments.tr)),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(TextEnum.uoHistoryScope.tr),
          if (data.$1.readOnly) Text(TextEnum.uoReadOnly.tr),
          IconButton(
            tooltip: TextEnum.uoRefresh.tr,
            onPressed: reload,
            icon: const Icon(Icons.refresh),
          ),
          if (data.$1.canEditUnits) ...[
            propertyField(reason, TextEnum.uoReason.tr, maxLength: 1000),
            PropertyAction(
              label: TextEnum.uoUpload.tr,
              action: () async {
                if (reason.text.trim().isEmpty)
                  throw const PropertyException('REASON_REQUIRED');
                final picked = await FilePicker.pickFiles(
                  type: FileType.custom,
                  allowedExtensions: ['pdf', 'jpg', 'jpeg', 'png'],
                );
                final path = picked?.files.single.path;
                if (path == null) return;
                await Get.find<UploadTransferFile>()(
                  widget.buildingId,
                  widget.unitId,
                  widget.transferId,
                  path,
                  reason.text.trim(),
                );
                reload();
              },
            ),
          ],
          if (data.$2.isEmpty)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Text(TextEnum.uoEmpty.tr),
            ),
          for (final f in data.$2)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(f.fileName),
                    Text('${f.sizeBytes} B · ${f.uploadedAt}'),
                    PropertyAction(
                      label: TextEnum.uoDownload.tr,
                      action: () async {
                        final bytes = await Get.find<DownloadTransferFile>()(
                          widget.buildingId,
                          widget.unitId,
                          widget.transferId,
                          f.id,
                        );
                        final name = f.fileName.replaceAll(
                          RegExp(r'[/\\]'),
                          '_',
                        );
                        final saved = await FilePicker.saveFile(
                          fileName: name,
                          bytes: Uint8List.fromList(bytes),
                        );
                        if (saved != null && context.mounted)
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(TextEnum.uoSaved.tr)),
                          );
                      },
                    ),
                    if (data.$1.canEditUnits)
                      PropertyAction(
                        label: TextEnum.uoRemove.tr,
                        action: () async {
                          if (reason.text.trim().isEmpty)
                            throw const PropertyException('REASON_REQUIRED');
                          if (!await confirmProperty(
                            context,
                            TextEnum.uoRemoveInfo.tr,
                          ))
                            return;
                          await Get.find<RemoveTransferFile>()(
                            widget.buildingId,
                            widget.unitId,
                            widget.transferId,
                            f.id,
                            reason.text.trim(),
                          );
                          reload();
                        },
                      ),
                  ],
                ),
              ),
            ),
        ],
      ),
    ),
  );
}
