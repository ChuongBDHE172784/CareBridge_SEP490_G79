import 'package:flutter/material.dart';
import '../../community/screens/post_answer_screen.dart';

class ExpertAnswerComposerScreen extends StatelessWidget {
  final String questionId;
  final String? questionTitle;
  final String? authorName;
  final String? topicName;
  final String? timeAgo;

  const ExpertAnswerComposerScreen({
    super.key,
    required this.questionId,
    this.questionTitle,
    this.authorName,
    this.topicName,
    this.timeAgo,
  });

  @override
  Widget build(BuildContext context) {
    return PostAnswerScreen(
      questionId: questionId,
      questionTitle: questionTitle,
      authorName: authorName,
      topicName: topicName,
      timeAgo: timeAgo,
    );
  }
}