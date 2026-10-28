package com.bgsistemas.api.globals;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.contact.ContactRepository;
import com.bgsistemas.api.contact.ContactStatus;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-302: whole application + H2. The search box and the status filter of the admin lists.
// Every name has "zqx" so the data of the seeder never matches
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    private SecurityUser admin;

    @BeforeEach
    void createAdmin() {
        admin = new SecurityUser(userRepository.save(new User("search-admin@test.com", "hashed", Role.ADMIN)));
    }

    @Test
    void contactsBySearchAndStatus() throws Exception {
        contactRepository.save(new Contact("Zqx Marta", "marta@mail.com", null, null, "Hi", true));
        Contact pedro = new Contact("Zqx Pedro", "pedro@mail.com", null, null, "Hi", true);
        pedro.setStatus(ContactStatus.CONTACTED);
        contactRepository.save(pedro);

        mockMvc.perform(get("/api/admin/contacts").param("q", " ZQX ").param("status", "CONTACTED").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Zqx Pedro"));
    }

    @Test
    void clientsTreatThePercentSignAsNormalText() throws Exception {
        clientRepository.save(new Client(null, "Zqx 100% Bar", "Bar", "Great", null, 5, true));
        clientRepository.save(new Client(null, "Zqx 1000 Bar", "Bar", "Great", null, 5, true));

        mockMvc.perform(get("/api/admin/clients").param("q", "zqx 100%").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Zqx 100% Bar"));
    }

    @Test
    void brandsBySearchAndActive() throws Exception {
        brandRepository.save(new Brand(null, "Zqx Soft", "Text", "software", null, true));
        brandRepository.save(new Brand(null, "Zqx Old", "Text", "software", null, false));

        mockMvc.perform(get("/api/admin/brands").param("q", "zqx").param("active", "false").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Zqx Old"));
    }

    @Test
    void productsByTheNameOfTheirBrand() throws Exception {
        Brand brand = brandRepository.save(new Brand(null, "Zqx Brand", "Text", "software", null, true));
        for (String name : new String[]{"Alpha", "Beta"}) {
            Service product = new Service(null, name, "Text", null);
            product.setBrand(brand);
            product.setActive(!"Beta".equals(name));
            serviceRepository.save(product);
        }

        mockMvc.perform(get("/api/admin/services").param("q", "zqx brand").param("active", "true").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"));
    }

    @Test
    void adminsBySearchAndStatus() throws Exception {
        User invited = new User("zqx-invited@test.com", "hashed", Role.ADMIN);
        invited.setStatus(UserStatus.PENDING);
        userRepository.save(invited);
        userRepository.save(new User("zqx-active@test.com", "hashed", Role.ADMIN));

        mockMvc.perform(get("/api/admin/users").param("q", "zqx").param("status", "PENDING").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("zqx-invited@test.com"));
    }

    @Test
    void theExportUsesTheSameFilters() throws Exception {
        brandRepository.save(new Brand(null, "Zqx Old", "Text", "software", null, false));

        mockMvc.perform(get("/api/admin/brands/export").param("format", "xlsx")
                        .param("q", "zqx").param("active", "false").with(user(admin)))
                .andExpect(status().isOk());
    }
}
