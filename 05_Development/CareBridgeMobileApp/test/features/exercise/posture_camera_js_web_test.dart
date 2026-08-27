@TestOn('browser')
library;

import 'dart:js_interop';
import 'dart:js_interop_unsafe';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/services/posture_camera_js_web.dart'
    as adapter;

void main() {
  test(
    'passes JS arguments to constructor, methods, and Promise bridge',
    () async {
      final functionConstructor = globalContext['Function'] as JSFunction;
      final poseConstructor =
          functionConstructor.callAsConstructorVarArgs<JSObject>(<JSAny?>[
                'options'.toJS,
                '''
            this.receivedOptions = options !== undefined;
            this.constructorArgCount = arguments.length;
            this.constructorFirstWasNull = options === null;
            this.setOptions = function(value) {
              this.modelComplexity = value.modelComplexity;
            };
            this.capture = function(value) {
              this.methodArgCount = arguments.length;
              this.methodFirstWasNull = value === null;
            };
            this.send = function() { return Promise.resolve(42); };
          '''
                    .toJS,
              ])
              as JSFunction;

      final pose = adapter.callConstructor<Object?>(poseConstructor, <Object?>[
        adapter.jsify(<String, Object?>{'locateFile': 'local'}),
      ]);
      expect(pose, isNotNull);
      expect(adapter.getProperty<bool>(pose!, 'receivedOptions'), isTrue);
      expect(adapter.getProperty<num>(pose, 'constructorArgCount'), 1);
      expect(
        adapter.getProperty<bool>(pose, 'constructorFirstWasNull'),
        isFalse,
      );

      adapter.callMethod(pose, 'setOptions', <Object?>[
        adapter.jsify(<String, Object?>{'modelComplexity': 1}),
      ]);
      expect(adapter.getProperty<num>(pose, 'modelComplexity'), 1);

      adapter.callMethod(pose, 'capture', <Object?>[null]);
      expect(adapter.getProperty<num>(pose, 'methodArgCount'), 1);
      expect(adapter.getProperty<bool>(pose, 'methodFirstWasNull'), isTrue);

      final nullPose = adapter.callConstructor<Object?>(
        poseConstructor,
        <Object?>[null],
      );
      expect(nullPose, isNotNull);
      expect(adapter.getProperty<num>(nullPose!, 'constructorArgCount'), 1);
      expect(
        adapter.getProperty<bool>(nullPose, 'constructorFirstWasNull'),
        isTrue,
      );

      final promise = adapter.callMethod(pose, 'send', const <Object?>[]);
      expect(promise, isNotNull);
      expect(await adapter.promiseToFuture<num>(promise!), 42);
    },
  );
}
