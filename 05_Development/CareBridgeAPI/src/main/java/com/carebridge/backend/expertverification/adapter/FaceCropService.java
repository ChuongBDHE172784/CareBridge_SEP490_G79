package com.carebridge.backend.expertverification.adapter;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class FaceCropService {

    private static final double PADDING_RATIO = 0.2; // 20% padding around face

    /**
     * Crops the face from an image using the provided bounding box.
     * Handles EXIF orientation, clamps coordinates to image bounds, adds padding.
     *
     * @param imageBytes Original image bytes
     * @param box Face bounding box (normalized 0-1 or pixel coordinates)
     * @param mimeType Image MIME type
     * @return Cropped face image bytes, or empty if crop failed
     */
    public Optional<byte[]> cropFace(byte[] imageBytes, FaceBoundingBox box, String mimeType) {
        try {
            // Load image
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                return Optional.empty();
            }

            // Handle EXIF orientation (JPEG)
            if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType)) {
                original = correctExifOrientation(imageBytes, original);
            }

            int imgWidth = original.getWidth();
            int imgHeight = original.getHeight();

            // Convert normalized coordinates to pixel coordinates if needed
            double xMin, yMin, width, height;
            if (box.normalized()) {
                // Normalized coordinates
                xMin = box.x() * imgWidth;
                yMin = box.y() * imgHeight;
                width = box.width() * imgWidth;
                height = box.height() * imgHeight;
            } else {
                // Already pixel coordinates
                xMin = box.x();
                yMin = box.y();
                width = box.width();
                height = box.height();
            }

            // Add padding
            double padX = width * PADDING_RATIO;
            double padY = height * PADDING_RATIO;

            xMin = Math.max(0, xMin - padX);
            yMin = Math.max(0, yMin - padY);
            width = Math.min(imgWidth - xMin, width + 2 * padX);
            height = Math.min(imgHeight - yMin, height + 2 * padY);

            // Clamp to integer bounds
            int x = (int) Math.round(xMin);
            int y = (int) Math.round(yMin);
            int w = (int) Math.round(width);
            int h = (int) Math.round(height);

            // Ensure minimum crop size (e.g., 64x64)
            if (w < 64 || h < 64) {
                return Optional.empty();
            }

            // Crop
            BufferedImage cropped = original.getSubimage(x, y, w, h);

            // Optionally resize to standard size for verification (e.g., 256x256)
            BufferedImage normalized = resizeForVerification(cropped);

            // Write to bytes
            String format = "image/jpeg".equals(mimeType) ? "jpg" : "png";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(normalized, format, out);
            return Optional.of(out.toByteArray());

        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    /**
     * Corrects EXIF orientation for JPEG images using metadata-extractor.
     * Strips GPS/EXIF metadata from the derived crop.
     */
    private BufferedImage correctExifOrientation(byte[] imageBytes, BufferedImage image) {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifDir == null) {
                return image;
            }

            int orientation = exifDir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            if (orientation <= 1) {
                return image; // No rotation needed
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // Determine transformed dimensions
            int newWidth, newHeight;
            if (orientation >= 5) { // 5, 6, 7, 8 - swapped dimensions
                newWidth = height;
                newHeight = width;
            } else {
                newWidth = width;
                newHeight = height;
            }

            BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
            Graphics2D g2d = rotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            switch (orientation) {
                case 2 -> { // Flip horizontal
                    g2d.translate(newWidth, 0);
                    g2d.scale(-1, 1);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 3 -> { // Rotate 180
                    g2d.translate(newWidth, newHeight);
                    g2d.rotate(Math.PI);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 4 -> { // Flip vertical
                    g2d.translate(0, newHeight);
                    g2d.scale(1, -1);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 5 -> { // Transpose (flip horizontal + rotate 90 CW)
                    g2d.rotate(Math.PI / 2);
                    g2d.translate(0, -newWidth);
                    g2d.scale(-1, 1);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 6 -> { // Rotate 90 CW
                    g2d.rotate(Math.PI / 2);
                    g2d.translate(0, -newWidth);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 7 -> { // Transverse (flip horizontal + rotate 90 CCW / 270 CW)
                    g2d.rotate(-Math.PI / 2);
                    g2d.translate(-newHeight, 0);
                    g2d.scale(-1, 1);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                case 8 -> { // Rotate 90 CCW / 270 CW
                    g2d.rotate(-Math.PI / 2);
                    g2d.translate(-newHeight, 0);
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
                default -> {
                    g2d.drawImage(image, 0, 0, width, height, null);
                }
            }
            g2d.dispose();

            // Clamp box if needed after rotation (handled by caller using normalized coords)
            return rotated;
        } catch (ImageProcessingException | IOException ex) {
            return image;
        }
    }

    /**
     * Resizes the cropped face to a standard size suitable for verification.
     * Maintains aspect ratio, pads with black if necessary.
     */
    private BufferedImage resizeForVerification(BufferedImage cropped) {
        final int TARGET_SIZE = 256;

        int srcW = cropped.getWidth();
        int srcH = cropped.getHeight();

        // Calculate scale to fit within target size while maintaining aspect ratio
        double scale = Math.min((double) TARGET_SIZE / srcW, (double) TARGET_SIZE / srcH);
        int newW = (int) (srcW * scale);
        int newH = (int) (srcH * scale);

        // Create padded image
        BufferedImage resized = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(0, 0, TARGET_SIZE, TARGET_SIZE);

        int x = (TARGET_SIZE - newW) / 2;
        int y = (TARGET_SIZE - newH) / 2;
        g2d.drawImage(cropped.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH), x, y, null);
        g2d.dispose();

        return resized;
    }
}