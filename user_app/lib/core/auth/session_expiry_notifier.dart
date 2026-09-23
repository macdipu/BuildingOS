import 'dart:async';

class SessionExpiryNotifier {
  final StreamController<void> _controller = StreamController<void>.broadcast();

  Stream<void> get onExpired => _controller.stream;

  void notifyExpired() => _controller.add(null);

  Future<void> dispose() => _controller.close();
}
