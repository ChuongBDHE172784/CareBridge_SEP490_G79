package com.carebridge.backend.expertverification.service;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CredentialIssuerService {

  public List<String> getAllIssuers() {
    return List.of(
      "Bộ Y tế",
      "Sở Y tế Hà Nội",
      "Sở Y tế TP Hồ Chí Minh",
      "Sở Y tế Đà Nẵng",
      "Sở Y tế khác",
      "Hội đồng Y khoa Việt Nam",
      "Trường Đại học Y Hà Nội",
      "Trường Đại học Y dược TP Hồ Chí Minh",
      "Trường Đại học Y dược Huế",
      "Trường Đại học Y dược Cần Thơ",
      "Học viện Y học cổ truyền Việt Nam",
      "Bộ Giáo dục và Đào tạo",
      "Trường Đại học khác",
      "Cơ quan đăng ký hành nghề y tế",
      "UBND tỉnh / thành phố",
      "Hội nghề nghiệp y tế",
      "Tổ chức y tế quốc tế",
      "Khác"
    );
  }
}
