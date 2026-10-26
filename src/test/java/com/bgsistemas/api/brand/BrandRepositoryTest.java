package com.bgsistemas.api.brand;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BrandRepositoryTest {

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void shouldSaveAndRetrieveBrand() {
        Brand brand = new Brand(null, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);

        Brand saved = brandRepository.save(brand);

        assertThat(saved.getId()).isNotNull();
        assertThat(brandRepository.findById(saved.getId())).isPresent();
    }

    // BGSI-302: admin list with search box (name or category) + active

    @Test
    void shouldSearchBrandsByNameOrCategoryAndActive() {
        brandRepository.save(new Brand(null, "BDP", "Software", "Software", null, true));
        brandRepository.save(new Brand(null, "PUDU", "Robots", "Robótica", null, true));
        brandRepository.save(new Brand(null, "Old Soft", "Old", "Software", null, false));

        assertThat(names(brandRepository.search("%pudu%", null, PageRequest.of(0, 10)))).containsExactly("PUDU");
        assertThat(names(brandRepository.search("%software%", null, PageRequest.of(0, 10))))
                .containsExactlyInAnyOrder("BDP", "Old Soft");
        assertThat(names(brandRepository.search("%software%", false, PageRequest.of(0, 10)))).containsExactly("Old Soft");
        assertThat(brandRepository.search(null, true, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
    }

    private List<String> names(Page<Brand> page) {
        return page.getContent().stream().map(Brand::getName).toList();
    }
}
