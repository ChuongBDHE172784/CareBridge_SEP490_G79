import 'dart:js_interop';

Object get globalThis => Object();

T? getProperty<T>(Object object, String property) => null;

Object? jsify(Object? value) => value;

T callConstructor<T>(Object constructor, List<Object?> arguments) {
  throw UnsupportedError('JavaScript interop is only available on Web');
}

T callMethod<T>(Object object, String method, List<Object?> arguments) {
  throw UnsupportedError('JavaScript interop is only available on Web');
}

Object allowInteropString(String Function(String) function) => function;

Object allowInteropResults(void Function(JSAny?) function) => function;

Future<T> promiseToFuture<T>(Object promise) {
  return Future<T>.error(
    UnsupportedError('JavaScript interop is only available on Web'),
  );
}
