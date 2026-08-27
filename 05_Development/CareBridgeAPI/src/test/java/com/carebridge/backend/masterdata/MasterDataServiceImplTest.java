package com.carebridge.backend.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.map.repository.CareFacilityRepository;
import com.carebridge.backend.masterdata.dto.response.MasterDataMapper;
import com.carebridge.backend.masterdata.entity.AdministrativeArea;
import com.carebridge.backend.masterdata.repository.AdministrativeAreaRepository;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import com.carebridge.backend.masterdata.service.impl.MasterDataServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterDataServiceImplTest {

    private static final UUID PROVINCE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID DISTRICT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Mock private AdministrativeAreaRepository administrativeAreaRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private CareFacilityRepository careFacilityRepository;

    private MasterDataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MasterDataServiceImpl(
                administrativeAreaRepository,
                specialtyRepository,
                careFacilityRepository,
                new MasterDataMapper());
    }

    @Test
    void getWardsByDistrictReadsOnlyCanonicalWardChildrenAndKeepsApiShape() {
        AdministrativeArea province = AdministrativeArea.builder()
                .id(PROVINCE_ID)
                .areaType("PROVINCE")
                .code("PROVINCE:01")
                .legacyCode("01")
                .name("Thành phố Hà Nội")
                .build();
        AdministrativeArea district = AdministrativeArea.builder()
                .id(DISTRICT_ID)
                .parentAreaId(PROVINCE_ID)
                .areaType("DISTRICT")
                .code("DISTRICT:0101")
                .legacyCode("0101")
                .name("Ba Đình")
                .build();
        AdministrativeArea ward = AdministrativeArea.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000103"))
                .parentAreaId(DISTRICT_ID)
                .areaType("WARD")
                .code("WARD:01001")
                .legacyCode("01001")
                .name("Phúc Xá")
                .nameEn("Phuc Xa")
                .build();
        when(administrativeAreaRepository.findByCode("DISTRICT:0101"))
                .thenReturn(Optional.of(district));
        when(administrativeAreaRepository.findById(PROVINCE_ID))
                .thenReturn(Optional.of(province));
        when(administrativeAreaRepository.findByAreaTypeAndParentAreaIdOrderByNameAsc(
                        "WARD", DISTRICT_ID))
                .thenReturn(List.of(ward));

        var result = service.getWardsByDistrict("0101");

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.getWardId()).isEqualTo("01001");
            assertThat(response.getDistrictId()).isEqualTo("0101");
            assertThat(response.getProvinceId()).isEqualTo("01");
            assertThat(response.getName()).isEqualTo("Phúc Xá");
            assertThat(response.getNameEn()).isEqualTo("Phuc Xa");
        });
        verify(administrativeAreaRepository)
                .findByAreaTypeAndParentAreaIdOrderByNameAsc("WARD", DISTRICT_ID);
    }

    @Test
    void getWardsByDistrictReturnsEmptyWithoutACompleteCanonicalHierarchy() {
        assertThat(service.getWardsByDistrict(" ")).isEmpty();
        verify(administrativeAreaRepository, never()).findByCode("DISTRICT: ");

        when(administrativeAreaRepository.findByCode("DISTRICT:9999"))
                .thenReturn(Optional.empty());

        assertThat(service.getWardsByDistrict("9999")).isEmpty();
        verify(administrativeAreaRepository, never())
                .findByAreaTypeAndParentAreaIdOrderByNameAsc(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(UUID.class));
    }
}
