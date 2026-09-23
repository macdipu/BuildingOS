import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:customer/app/config/app_flavour.dart';
import 'package:customer/app/app.dart';

void main(List<String> args) async {
  WidgetsFlutterBinding.ensureInitialized();

  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
  ]);

  await bootstrap(() => const MyApp());
}
