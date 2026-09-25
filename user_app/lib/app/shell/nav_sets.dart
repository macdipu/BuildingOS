/// BRD §37 bottom navigation sets, chosen per role in the active building.
enum ShellTab { dashboard, home, finance, units, work, community, payments, properties, profile, more }

enum NavRole { manager, committee, owner, tenant }

const Map<NavRole, List<ShellTab>> navSets = {
  NavRole.manager: [ShellTab.dashboard, ShellTab.finance, ShellTab.units, ShellTab.work, ShellTab.more],
  NavRole.committee: [ShellTab.dashboard, ShellTab.finance, ShellTab.work, ShellTab.community, ShellTab.more],
  NavRole.owner: [ShellTab.dashboard, ShellTab.properties, ShellTab.payments, ShellTab.community, ShellTab.more],
  NavRole.tenant: [ShellTab.home, ShellTab.payments, ShellTab.community, ShellTab.profile],
};

const Map<String, NavRole> _roleMap = {
  'BUILDING_ADMIN': NavRole.manager,
  'PROPERTY_MANAGER': NavRole.manager,
  'RENT_MANAGER': NavRole.manager,
  'COMMITTEE': NavRole.committee,
  'OWNER': NavRole.owner,
  'TENANT': NavRole.tenant,
};

/// BOS-011 Q-01: several roles resolve by precedence manager > committee > owner > tenant.
/// No recognised role falls back to the least-privileged (tenant) set.
NavRole navRoleFor(Iterable<String> roles) {
  final mapped = roles.map((r) => _roleMap[r.toUpperCase()]).whereType<NavRole>().toSet();
  for (final role in NavRole.values) {
    if (mapped.contains(role)) return role;
  }
  return NavRole.tenant;
}

List<ShellTab> navFor(Iterable<String> roles) => navSets[navRoleFor(roles)] ?? const [];
