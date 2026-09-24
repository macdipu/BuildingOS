import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../domain/entities/application_document.dart';
import '../controllers/application_detail_controller.dart';
import 'confirm_dialog.dart';

class DocumentsSection extends StatelessWidget {
  const DocumentsSection({super.key, required this.editable});

  final bool editable;

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationDetailController>();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(
                child: Text(TextEnum.documents.tr,
                    style: Theme.of(context).textTheme.titleMedium)),
            if (editable) const _AddDocumentButton(),
          ],
        ),
        Obx(() {
          final documents = controller.documents.toList();
          return Column(
            children: [
              if (controller.isUploading.value) const LinearProgressIndicator(),
              if (documents.isEmpty)
                Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Text(TextEnum.noDocuments.tr)),
              for (final document in documents)
                _DocumentTile(document: document, editable: editable),
            ],
          );
        }),
      ],
    );
  }
}

class _AddDocumentButton extends StatelessWidget {
  const _AddDocumentButton();

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationDetailController>();
    return Obx(() => PopupMenuButton<DocumentSource>(
          enabled: !controller.isUploading.value,
          tooltip: TextEnum.addDocument.tr,
          onSelected: controller.addDocument,
          itemBuilder: (_) => [
            PopupMenuItem(
                value: DocumentSource.camera,
                child: Text(TextEnum.takePhoto.tr)),
            PopupMenuItem(
                value: DocumentSource.gallery,
                child: Text(TextEnum.chooseFromGallery.tr)),
            PopupMenuItem(
                value: DocumentSource.file,
                child: Text(TextEnum.chooseFile.tr)),
          ],
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              const Icon(Icons.attach_file),
              const SizedBox(width: 4),
              Text(TextEnum.addDocument.tr),
            ]),
          ),
        ));
  }
}

class _DocumentTile extends StatelessWidget {
  const _DocumentTile({required this.document, required this.editable});

  final ApplicationDocument document;
  final bool editable;

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationDetailController>();
    final kb = (document.sizeBytes / 1024).ceil();
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(document.contentType == 'application/pdf'
          ? Icons.picture_as_pdf_outlined
          : Icons.image_outlined),
      title: Text(document.fileName, overflow: TextOverflow.ellipsis),
      subtitle: Text('$kb KB'),
      trailing: editable
          ? IconButton(
              tooltip: TextEnum.removeDocument.tr,
              icon: const Icon(Icons.delete_outline),
              onPressed: () async {
                final ok = await confirm(context,
                    message: TextEnum.confirmRemoveDocument.tr,
                    confirmLabel: TextEnum.removeDocument.tr);
                if (ok) await controller.removeDocument(document);
              },
            )
          : null,
    );
  }
}
