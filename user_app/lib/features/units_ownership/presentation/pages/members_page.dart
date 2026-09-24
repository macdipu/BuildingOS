import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';

class MembersPage extends StatefulWidget {
  const MembersPage({super.key, required this.buildingId});
  final String buildingId;
  @override
  State<MembersPage> createState() => _MembersPageState();
}

class _MembersPageState extends State<MembersPage> {
  final phone = TextEditingController(), reason = TextEditingController();
  final form = GlobalKey<FormState>();
  @override
  void dispose() {
    phone.dispose();
    reason.dispose();
    super.dispose();
  }

  Future<(BuildingAccess, List<BuildingMember>, List<OwnerInvitation>)>
  load() async {
    final b = widget.buildingId;
    return (
      await Get.find<GetBuildingAccess>()(b),
      await Get.find<ListBuildingMembers>()(b),
      await Get.find<ListBuildingInvitations>()(b),
    );
  }

  Future<String?> revokeReason() async {
    final c = TextEditingController();
    final key = GlobalKey<FormState>();
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(TextEnum.uoRevoke.tr),
        content: Form(
          key: key,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(TextEnum.uoRevokeInfo.tr),
              propertyField(
                c,
                TextEnum.uoReason.tr,
                validator: requiredText,
                maxLength: 1000,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(TextEnum.cancel.tr),
          ),
          FilledButton(
            onPressed: () {
              if (key.currentState!.validate())
                Navigator.pop(ctx, c.text.trim());
            },
            child: Text(TextEnum.uoConfirm.tr),
          ),
        ],
      ),
    );
    // The dialog's closing animation still owns the controller until the next frame.
    WidgetsBinding.instance.addPostFrameCallback((_) => c.dispose());
    return result;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoMembers.tr)),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          IconButton(
            tooltip: TextEnum.uoRefresh.tr,
            onPressed: reload,
            icon: const Icon(Icons.refresh),
          ),
          if (data.$1.readOnly) Text(TextEnum.uoReadOnly.tr),
          if (data.$1.canEditMembers)
            Form(
              key: form,
              child: Column(
                children: [
                  Text(TextEnum.uoClaimInfo.tr),
                  propertyField(
                    phone,
                    TextEnum.uoPhone.tr,
                    keyboard: TextInputType.phone,
                    validator: (v) =>
                        RegExp(
                          r'^(?:\+?88)?01[3-9]\d{8}$',
                        ).hasMatch(v?.trim() ?? '')
                        ? null
                        : TextEnum.errInvalidPhone.tr,
                  ),
                  propertyField(
                    reason,
                    TextEnum.uoReason.tr,
                    validator: requiredText,
                    maxLength: 1000,
                  ),
                  PropertyAction(
                    label: TextEnum.uoInvite.tr,
                    action: () async {
                      if (!form.currentState!.validate()) return;
                      await Get.find<InviteBuildingOwner>()(
                        widget.buildingId,
                        phone.text.trim(),
                        reason.text.trim(),
                      );
                      phone.clear();
                      reason.clear();
                      reload();
                    },
                  ),
                ],
              ),
            ),
          const SizedBox(height: 24),
          Text(
            TextEnum.uoInvitations.tr,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          if (data.$3.isEmpty) Text(TextEnum.uoEmpty.tr),
          for (final i in data.$3)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(i.label),
                    Text(
                      '${propertyLabel(i.status)} · ${TextEnum.uoExpires.tr}: ${i.expiresAt}',
                    ),
                    if (data.$1.canEditMembers && i.status == 'PENDING')
                      PropertyAction(
                        label: TextEnum.uoRevoke.tr,
                        action: () async {
                          final r = await revokeReason();
                          if (r == null) return;
                          await Get.find<RevokeOwnerInvitation>()(
                            widget.buildingId,
                            i.id,
                            r,
                          );
                          reload();
                        },
                      ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 24),
          Text(
            TextEnum.uoMembers.tr,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          for (final m in data.$2)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SelectableText(m.userId),
                    Text(
                      '${propertyLabel(m.role)} · ${propertyLabel(m.status)}',
                    ),
                    if (data.$1.canEditMembers &&
                        m.role == 'OWNER' &&
                        m.status == 'ACTIVE')
                      PropertyAction(
                        label: TextEnum.uoRevoke.tr,
                        action: () async {
                          final r = await revokeReason();
                          if (r == null) return;
                          await Get.find<RevokeOwnerMembership>()(
                            widget.buildingId,
                            m.id,
                            m.version,
                            r,
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
