import 'package:flutter/material.dart';

class IntroHeader extends StatelessWidget {
  const IntroHeader({
    super.key,
    required this.title,
    this.introText,
    this.localeSwitch,
  });

  final String title;
  final String? introText;
  final Widget? localeSwitch;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final intro = introText;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(child: Text(title, style: textTheme.headlineMedium)),
            localeSwitch ?? const SizedBox.shrink(),
          ],
        ),
        if (intro != null) ...[
          const SizedBox(height: 8),
          Text(intro, style: textTheme.titleMedium),
        ],
      ],
    );
  }
}
