import 'package:customer/app/shell/nav_sets.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('BRD §37 nav sets', () {
    test('building admin gets Dashboard/Finance/Units/Work/More', () {
      expect(navFor(['BUILDING_ADMIN']),
          [ShellTab.dashboard, ShellTab.finance, ShellTab.units, ShellTab.work, ShellTab.more]);
    });

    test('owner gets Dashboard/Properties/Payments/Community/More', () {
      expect(navFor(['OWNER']),
          [ShellTab.dashboard, ShellTab.properties, ShellTab.payments, ShellTab.community, ShellTab.more]);
    });

    test('committee and tenant sets', () {
      expect(navFor(['COMMITTEE']),
          [ShellTab.dashboard, ShellTab.finance, ShellTab.work, ShellTab.community, ShellTab.more]);
      expect(navFor(['TENANT']), [ShellTab.home, ShellTab.payments, ShellTab.community, ShellTab.profile]);
    });
  });

  group('Q-01 precedence', () {
    test('manager > committee > owner > tenant regardless of order', () {
      expect(navRoleFor(['OWNER', 'BUILDING_ADMIN']), NavRole.manager);
      expect(navRoleFor(['TENANT', 'COMMITTEE', 'OWNER']), NavRole.committee);
      expect(navRoleFor(['TENANT', 'OWNER']), NavRole.owner);
    });

    test('unknown or no roles fall back to the tenant set', () {
      expect(navRoleFor(['VIEWER']), NavRole.tenant);
      expect(navRoleFor(const []), NavRole.tenant);
    });
  });
}
