import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mime/mime.dart';

import '../../../core/network/api_client.dart';

const int communityImageLimit = 3;
const int _maxCommunityImageBytes = 10 * 1024 * 1024;

class CommunityImageAttachment {
  final Uint8List bytes;
  final String fileName;
  final String mimeType;

  const CommunityImageAttachment({
    required this.bytes,
    required this.fileName,
    required this.mimeType,
  });
}

class CommunityImageService {
  CommunityImageService({ImagePicker? picker})
    : _picker = picker ?? ImagePicker();

  final ImagePicker _picker;

  Future<CommunityImageAttachment?> pick(ImageSource source) async {
    final image = await _picker.pickImage(
      source: source,
      imageQuality: 85,
      maxWidth: 1800,
      maxHeight: 1800,
    );
    if (image == null) return null;

    final bytes = await image.readAsBytes();
    if (bytes.length > _maxCommunityImageBytes) {
      throw const FormatException('Mỗi ảnh phải nhỏ hơn 10 MB');
    }

    final mimeType =
        image.mimeType ??
        lookupMimeType(image.name, headerBytes: bytes.take(32).toList()) ??
        'image/jpeg';
    if (!mimeType.startsWith('image/')) {
      throw const FormatException('Tệp đã chọn không phải là hình ảnh');
    }

    return CommunityImageAttachment(
      bytes: bytes,
      fileName: image.name,
      mimeType: mimeType,
    );
  }

  Future<List<String>> uploadAll(
    List<CommunityImageAttachment> images, {
    required String purpose,
  }) async {
    final urls = <String>[];
    for (final image in images) {
      final response = await apiMultipart(
        '/api/v1/files/upload/with-purpose?kind=IMAGE&purpose=$purpose&accessMode=PUBLIC',
        const {},
        files: [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: image.bytes,
            fileName: image.fileName,
            mimeType: image.mimeType,
          ),
        ],
      );
      final url = response?['data']?['presignedUrl'] as String?;
      if (url == null || !url.startsWith('https://res.cloudinary.com/')) {
        throw const FormatException('Cloudinary không trả về URL ảnh hợp lệ');
      }
      urls.add(url);
    }
    return urls;
  }
}

class CommunityImagePickerField extends StatelessWidget {
  final List<CommunityImageAttachment> images;
  final List<String> existingImageUrls;
  final VoidCallback onCamera;
  final VoidCallback onGallery;
  final ValueChanged<int> onRemove;
  final ValueChanged<int>? onRemoveExisting;
  final bool enabled;

  const CommunityImagePickerField({
    super.key,
    required this.images,
    this.existingImageUrls = const [],
    required this.onCamera,
    required this.onGallery,
    required this.onRemove,
    this.onRemoveExisting,
    this.enabled = true,
  });

  @override
  Widget build(BuildContext context) {
    final imageCount = existingImageUrls.length + images.length;
    final canAdd = enabled && imageCount < communityImageLimit;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(
              child: Text(
                'Hình ảnh (tối đa 3)',
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
            Text(
              '$imageCount/$communityImageLimit',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton.icon(
              key: const Key('community-camera-button'),
              onPressed: canAdd ? onCamera : null,
              icon: const Icon(Icons.photo_camera_outlined),
              label: const Text('Chụp ảnh'),
            ),
            OutlinedButton.icon(
              key: const Key('community-gallery-button'),
              onPressed: canAdd ? onGallery : null,
              icon: const Icon(Icons.photo_library_outlined),
              label: const Text('Thư viện'),
            ),
          ],
        ),
        if (imageCount > 0) ...[
          const SizedBox(height: 10),
          SizedBox(
            height: 92,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: imageCount,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final isExisting = index < existingImageUrls.length;
                final localIndex = index - existingImageUrls.length;
                return Stack(
                  clipBehavior: Clip.none,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(10),
                      child: isExisting
                          ? Image.network(
                              existingImageUrls[index],
                              width: 92,
                              height: 92,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) => Container(
                                width: 92,
                                height: 92,
                                color: Colors.black12,
                                alignment: Alignment.center,
                                child: const Icon(Icons.broken_image_outlined),
                              ),
                            )
                          : Image.memory(
                              images[localIndex].bytes,
                              width: 92,
                              height: 92,
                              fit: BoxFit.cover,
                            ),
                    ),
                    Positioned(
                      right: -6,
                      top: -6,
                      child: IconButton.filled(
                        key: Key(
                          isExisting
                              ? 'community-remove-existing-image-$index'
                              : 'community-remove-image-$localIndex',
                        ),
                        constraints: const BoxConstraints.tightFor(
                          width: 28,
                          height: 28,
                        ),
                        padding: EdgeInsets.zero,
                        iconSize: 16,
                        onPressed: !enabled
                            ? null
                            : isExisting
                            ? (onRemoveExisting == null
                                  ? null
                                  : () => onRemoveExisting!(index))
                            : () => onRemove(localIndex),
                        icon: const Icon(Icons.close),
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
        ],
      ],
    );
  }
}

class CommunityNetworkImageGallery extends StatelessWidget {
  final List<String> imageUrls;

  const CommunityNetworkImageGallery({super.key, required this.imageUrls});

  @override
  Widget build(BuildContext context) {
    if (imageUrls.isEmpty) return const SizedBox.shrink();
    return SizedBox(
      height: 180,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: imageUrls.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final url = imageUrls[index];
          return InkWell(
            onTap: () => showDialog<void>(
              context: context,
              builder: (_) => Dialog(
                backgroundColor: Colors.black,
                insetPadding: const EdgeInsets.all(12),
                child: InteractiveViewer(
                  child: Image.network(url, fit: BoxFit.contain),
                ),
              ),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Image.network(
                url,
                width: imageUrls.length == 1 ? 280 : 180,
                height: 180,
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => Container(
                  width: 180,
                  color: Colors.black12,
                  alignment: Alignment.center,
                  child: const Icon(Icons.broken_image_outlined),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
