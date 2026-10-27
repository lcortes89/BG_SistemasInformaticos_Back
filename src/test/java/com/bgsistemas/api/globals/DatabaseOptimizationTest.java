package com.bgsistemas.api.globals;

import com.bgsistemas.api.adminuser.AdminUserService;
import com.bgsistemas.api.brand.BrandServiceImpl;
import com.bgsistemas.api.client.ClientServiceImpl;
import com.bgsistemas.api.contact.ContactServiceImpl;
import com.bgsistemas.api.service.ServiceServiceImpl;
import com.bgsistemas.api.setting.SiteSettingServiceImpl;
import com.bgsistemas.api.summary.SummaryServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-326: the optimizations of the database stay in place
@DataJpaTest
class DatabaseOptimizationTest {

    @Autowired
    private EntityManager entityManager;

    // The methods that only read run in read-only transactions (less work for Hibernate and the database)
    @Test
    void theMethodsThatOnlyReadUseReadOnlyTransactions() {
        Map<Class<?>, List<String>> readers = Map.of(
                BrandServiceImpl.class, List.of("getActiveBrands", "getBrandById", "getBrandsPage", "countServices"),
                ClientServiceImpl.class, List.of("getPublishedClients", "getClientsPage", "getClientById"),
                ServiceServiceImpl.class, List.of("getAllServices", "getServicesByBrandId", "getFeaturedServices",
                        "getVisibleServicesPage", "getServicesPage", "getServiceById"),
                ContactServiceImpl.class, List.of("getContacts"),
                SummaryServiceImpl.class, List.of("getSummary"),
                SiteSettingServiceImpl.class, List.of("getHeroImageUrl"),
                AdminUserService.class, List.of("list"));

        readers.forEach((type, names) -> names.forEach(name -> {
            Method method = Arrays.stream(type.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(name))
                    .findFirst().orElseThrow();
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional).as(type.getSimpleName() + "." + name).isNotNull();
            assertThat(transactional.readOnly()).as(type.getSimpleName() + "." + name).isTrue();
        }));
    }

    @Test
    void theIndexesAreCreated() {
        @SuppressWarnings("unchecked")
        List<String> indexes = entityManager
                .createNativeQuery("SELECT LOWER(INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES")
                .getResultList();

        assertThat(indexes).contains("idx_contact_status_created", "idx_contact_created",
                "idx_password_reset_expires", "idx_email_change_expires");
    }
}
