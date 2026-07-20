import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/healthRecords/models/postpartum_log_model.dart';
import 'package:untitled/features/healthRecords/screens/postpartum_log_list_screen.dart';
import 'package:untitled/features/healthRecords/services/postpartum_log_service.dart';

void main() {
  testWidgets('refresh supersedes an older load-more response', (tester) async {
    final service = _RacingPostpartumLogService();
    await tester.pumpWidget(
      MaterialApp(
        home: PostpartumLogListScreen(
          journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('Đau 1/10'), findsOneWidget);
    await tester.tap(find.text('Tải thêm'));
    await tester.pump();

    unawaited(
      tester.state<RefreshIndicatorState>(find.byType(RefreshIndicator)).show(),
    );
    await tester.pump();
    service.completeRefresh(_page(pain: 2, hasNext: false));
    await tester.pumpAndSettle();

    service.completeLoadMore(_page(pain: 9, page: 1, hasNext: false));
    await tester.pumpAndSettle();

    expect(find.textContaining('Đau 2/10'), findsOneWidget);
    expect(find.textContaining('Đau 1/10'), findsNothing);
    expect(find.textContaining('Đau 9/10'), findsNothing);
  });
}

class _RacingPostpartumLogService extends PostpartumLogService {
  final _loadMore = Completer<PostpartumLogPage>();
  final _refresh = Completer<PostpartumLogPage>();
  var _pageZeroCalls = 0;

  void completeLoadMore(PostpartumLogPage value) => _loadMore.complete(value);
  void completeRefresh(PostpartumLogPage value) => _refresh.complete(value);

  @override
  Future<PostpartumLogPage> list(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) {
    if (page == 1) return _loadMore.future;
    _pageZeroCalls++;
    if (_pageZeroCalls == 1) {
      return Future.value(_page(pain: 1, hasNext: true));
    }
    return _refresh.future;
  }
}

PostpartumLogPage _page({
  required int pain,
  int page = 0,
  required bool hasNext,
}) => PostpartumLogPage(
  items: [
    PostpartumLog(
      id: 'cccccccc-0000-4000-8000-0000000000$pain',
      journeyId: 'bbbbbbbb-0000-4000-8000-000000000028',
      logDate: DateTime(2026, 7, 19),
      painLevel: pain,
    ),
  ],
  page: page,
  totalPages: hasNext ? page + 2 : page + 1,
  totalElements: hasNext ? 2 : 1,
);
