// The dynamic MediaPipe CDN boundary has no static Dart type; these casts are
// intentionally isolated in this adapter and never cross the feature API.
// ignore_for_file: invalid_runtime_check_with_js_interop_types

import 'dart:js_interop';
import 'dart:js_interop_unsafe';

Object get globalThis => globalContext;

T? getProperty<T>(Object object, String property) {
  if (object is Map) return object[property] as T?;
  if (object is List) {
    if (property == 'length') return object.length as T;
    final index = int.tryParse(property);
    return index != null && index >= 0 && index < object.length
        ? object[index] as T?
        : null;
  }
  final value = (object as JSObject)[property];
  if (value == null) return null;
  final dartValue = value.dartify();
  return (dartValue ?? value) as T?;
}

Object? jsify(Object? value) => value.jsify();

T callConstructor<T>(Object constructor, List<Object?> arguments) {
  final jsArguments = _externalizeArguments(arguments);
  return (constructor as JSFunction).callAsConstructorVarArgs<JSObject>(
        jsArguments,
      )
      as T;
}

Object? callMethod(Object object, String method, List<Object?> arguments) {
  final jsArguments = _externalizeArguments(arguments);
  return (object as JSObject).callMethodVarArgs<JSAny?>(
    method.toJS,
    jsArguments,
  );
}

List<JSAny?> _externalizeArguments(List<Object?> arguments) => <JSAny?>[
  for (final argument in arguments) argument.jsify(),
];

Object allowInteropString(String Function(String) function) => function.toJS;

Object allowInteropResults(void Function(JSAny?) function) => function.toJS;

Future<T> promiseToFuture<T>(Object promise) async {
  final result = await (promise as JSPromise<JSAny?>).toDart;
  final dartResult = result.dartify();
  return (dartResult ?? result) as T;
}
