import 'package:customer/core/usecases/usecase.dart';
import 'package:customer/core/utils/logger.dart';
import 'package:customer/core/utils/task_runner.dart';
import 'package:flutter/services.dart';


/// A service to handle clipboard operations.
class CopyService {
  CopyService._();
  static final CopyService instance = CopyService._();

  /// Copy text to the system clipboard.
  ResultFuture<void> copy(String text) async {
    return runTask(() async {
      await Clipboard.setData(ClipboardData(text: text));
      AppLogger.info('Text copied to clipboard');
    });
  }
}
