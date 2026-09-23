import 'package:customer/core/widgets/images/app_svg.dart';
import 'package:customer/res/resources.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(svg.cache.clear);

  test('precache stores the logo under the key SvgPicture.asset looks up',
      () async {
    final path = Resources.drawable.splashImage;

    await AppSvg.precache(path);

    expect(svg.cache.count, 1);
    var reloaded = false;
    await svg.cache.putIfAbsent(SvgAssetLoader(path).cacheKey(null), () {
      reloaded = true;
      return Future.value(ByteData(0));
    });
    expect(reloaded, isFalse);
  });
}
