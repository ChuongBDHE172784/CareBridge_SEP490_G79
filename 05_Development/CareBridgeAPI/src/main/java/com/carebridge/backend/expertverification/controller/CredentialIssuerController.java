package com.carebridge.backend.expertverification.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.expertverification.service.CredentialIssuerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/credential-issuers")
@RequiredArgsConstructor
public class CredentialIssuerController {

  private final CredentialIssuerService issuerService;

  @GetMapping
  public ApiResponse<List<String>> getIssuers() {
    return ApiResponse.success(issuerService.getAllIssuers());
  }
}
