import 'package:customer/app/shell/nav_sets.dart';
import 'package:customer/app/shell/shell_tabs.dart';
import 'package:customer/features/units_ownership/presentation/pages/building_units_page.dart';
import 'package:customer/features/units_ownership/presentation/pages/property_home_page.dart';
import 'package:customer/features/units_ownership/presentation/property_pages.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'app_shell_controller.dart';

/// BRD §37 per-role bottom navigation (BOS-011 D-02), styled after the Stitch mobile bar.
class AppShell extends StatelessWidget {
  const AppShell({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<AppShellController>();

    return Obx(() {
      final building = controller.activeBuilding.building.value;
      final tabs = controller.tabs.toList();
      if (building == null || tabs.isEmpty) {
        // No active building (e.g. deep link): pick one first.
        WidgetsBinding.instance.addPostFrameCallback((_) => Get.offAllNamed(PropertyPages.home));
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      }
      final index = controller.currentIndex.value.clamp(0, tabs.length - 1);
      return Scaffold(
        body: IndexedStack(
          index: index,
          children: [for (final tab in tabs) _body(tab, building.id, building.name)],
        ),
        bottomNavigationBar: DecoratedBox(
          decoration: BoxDecoration(
            border: Border(top: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
          ),
          child: NavigationBar(
            selectedIndex: index,
            onDestinationSelected: controller.changeTab,
            destinations: [
              for (final tab in tabs)
                NavigationDestination(
                  key: ValueKey('nav-${tab.name}'),
                  icon: Icon(shellTabIcon(tab)),
                  selectedIcon: Icon(shellTabSelectedIcon(tab)),
                  label: shellTabLabel(tab),
                ),
            ],
          ),
        ),
      );
    });
  }

  Widget _body(ShellTab tab, String buildingId, String buildingName) => switch (tab) {
        ShellTab.dashboard || ShellTab.home => DashboardTab(buildingName: buildingName),
        ShellTab.units => BuildingUnitsPage(buildingId: buildingId),
        ShellTab.properties => const PropertyHomePage(),
        ShellTab.more => const MoreTab(),
        _ => ComingSoonTab(title: shellTabLabel(tab)),
      };
}
