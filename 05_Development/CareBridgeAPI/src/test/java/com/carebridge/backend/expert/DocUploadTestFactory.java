package com.carebridge.backend.expert;

import com.carebridge.backend.expert.entity.VerificationDocType;
import java.util.UUID;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class DocUploadTestFactory {

  public static MultipartFile makePdfDoc() {
    return new MockMultipartFile(
        "file", "degree.pdf", "application/pdf", new byte[1024]);
  }

  public static MultipartFile makeLargeFile() {
    return new MockMultipartFile(
        "file", "big.pdf", "application/pdf", new byte[21 * 1024 * 1024]);
  }

  public static MultipartFile makeInvalidFile() {
    return new MockMultipartFile(
        "file", "virus.exe", "application/octet-stream", new byte[1024]);
  }

  public static MultipartFile makeJpegDoc() {
    return new MockMultipartFile(
        "file", "cert.jpg", "image/jpeg", new byte[2048]);
  }

  public static VerificationDocType randomDocType() {
    return VerificationDocType.values()[new java.util.Random().nextInt(VerificationDocType.values().length)];
  }

  public static UUID randomExpertId() {
    return UUID.randomUUID();
  }

  public static UUID randomAccountId() {
    return UUID.randomUUID();
  }
}
