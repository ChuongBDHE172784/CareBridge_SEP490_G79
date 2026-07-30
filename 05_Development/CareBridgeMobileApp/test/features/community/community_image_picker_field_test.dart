import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/widgets/community_image_attachments.dart';

void main() {
  testWidgets('edit image picker counts and removes an existing image', (
    tester,
  ) async {
    int? removedIndex;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: CommunityImagePickerField(
            images: const [],
            existingImageUrls: const [
              'https://res.cloudinary.com/demo/image/upload/question.jpg',
            ],
            onCamera: () {},
            onGallery: () {},
            onRemove: (_) {},
            onRemoveExisting: (index) => removedIndex = index,
          ),
        ),
      ),
    );

    expect(find.text('1/3'), findsOneWidget);
    await tester.tap(
      find.byKey(const Key('community-remove-existing-image-0')),
    );
    expect(removedIndex, 0);
  });
}
