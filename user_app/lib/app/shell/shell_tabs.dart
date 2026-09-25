import 'package:customer/app/shell/nav_sets.dart';
import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/features/building_application/presentation/building_application_pages.dart';
import 'package:customer/features/units_ownership/presentation/property_pages.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

String shellTabLabel(ShellTab tab) => switch (tab) {
      ShellTab.dashboard => TextEnum.navDashboard.tr,
      ShellTab.home => TextEnum.navHome.tr,
      ShellTab.finance => TextEnum.navFinance.tr,
      ShellTab.units => TextEnum.navUnits.tr,
      ShellTab.work => TextEnum.navWork.tr,
      ShellTab.community => TextEnum.navCommunity.tr,
      ShellTab.payments => TextEnum.navPayments.tr,
      ShellTab.properties => TextEnum.navProperties.tr,
      ShellTab.profile => TextEnum.navProfile.tr,
      ShellTab.more => TextEnum.navMore.tr,
    };

IconData shellTabIcon(ShellTab tab) => switch (tab) {
      ShellTab.dashboard || ShellTab.home => Icons.dashboard_outlined,
      ShellTab.finance => Icons.account_balance_wallet_outlined,
      ShellTab.units => Icons.apartment_outlined,
      ShellTab.work => Icons.construction_outlined,
      ShellTab.community => Icons.forum_outlined,
      ShellTab.payments => Icons.payments_outlined,
      ShellTab.properties => Icons.domain_outlined,
      ShellTab.profile => Icons.person_outline,
      ShellTab.more => Icons.more_horiz,
    };

IconData shellTabSelectedIcon(ShellTab tab) => switch (tab) {
      ShellTab.dashboard || ShellTab.home => Icons.dashboard,
      ShellTab.finance => Icons.account_balance_wallet,
      ShellTab.units => Icons.apartment,
      ShellTab.work => Icons.construction,
      ShellTab.community => Icons.forum,
      ShellTab.payments => Icons.payments,
      ShellTab.properties => Icons.domain,
      ShellTab.profile => Icons.person,
      ShellTab.more => Icons.more_horiz,
    };

/// BOS-011 Q-02: modules not built yet keep their §37 tab and show this placeholder.
class ComingSoonTab extends StatelessWidget {
  const ComingSoonTab({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(title)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Icon(Icons.hourglass_empty, size: 48, color: context.onSurfaceVariant),
              const SizedBox(height: 16),
              Text(TextEnum.comingSoonTitle.tr, style: context.headlineSmall),
              const SizedBox(height: 8),
              Text(
                TextEnum.comingSoonBody.tr,
                textAlign: TextAlign.center,
                style: context.bodyMedium?.copyWith(color: context.onSurfaceVariant),
              ),
            ]),
          ),
        ),
      );
}

/// Role dashboards are BOS-008; until then the tab names the active building and
/// keeps the entry points the starter home offered.
class DashboardTab extends StatelessWidget {
  const DashboardTab({super.key, required this.buildingName});

  final String buildingName;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(buildingName)),
        body: ListView(padding: const EdgeInsets.all(16), children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(children: [
                Icon(Icons.hourglass_empty, color: context.onSurfaceVariant),
                const SizedBox(width: 12),
                Expanded(child: Text(TextEnum.comingSoonBody.tr, style: context.bodyMedium)),
              ]),
            ),
          ),
        ]),
      );
}

class MoreTab extends StatelessWidget {
  const MoreTab({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(TextEnum.navMore.tr)),
        body: ListView(children: [
          ListTile(
            leading: const Icon(Icons.swap_horiz),
            title: Text(TextEnum.switchBuilding.tr),
            onTap: () => Get.toNamed(PropertyPages.home),
          ),
          ListTile(
            leading: const Icon(Icons.apartment_outlined),
            title: Text(TextEnum.buildingApplications.tr),
            onTap: () => Get.toNamed(BuildingApplicationPages.list),
          ),
        ]),
      );
}
