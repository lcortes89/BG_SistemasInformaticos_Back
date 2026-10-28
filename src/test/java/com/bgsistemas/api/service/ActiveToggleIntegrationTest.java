package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-309: whole application + H2. What the admin inactivates disappears from the public website
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiveToggleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    private SecurityUser admin;
    private Brand brand;
    private Service product;
    private Client client;

    @BeforeEach
    void createData() {
        admin = new SecurityUser(userRepository.save(new User("toggle-admin@test.com", "hashed", Role.ADMIN)));
        brand = brandRepository.save(new Brand(null, "Toggle Brand", "Software", "software", null, true));
        product = new Service(null, "Toggle Product", "Text", null);
        product.setFeatured(true);
        product.setBrand(brand);
        product = serviceRepository.save(product);
        client = clientRepository.save(new Client(null, "Toggle Client", "Bar", "Great", null, 5, true));
    }

    private ResultActions setActive(String type, Long id, boolean active) throws Exception {
        return mockMvc.perform(patch("/api/admin/" + type + "/" + id + "/active").with(user(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{ \"active\": " + active + " }"));
    }

    @Test
    void anInactiveProductIsHiddenEverywhereOnTheWebsite() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$[*].name", hasItem("Toggle Product")));

        setActive("services", product.getId(), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Toggle Product"))));
        mockMvc.perform(get("/api/services").param("featured", "true"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Toggle Product"))));
        mockMvc.perform(get("/api/brands/" + brand.getId() + "/services"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // The admin still sees it, marked as inactive
        mockMvc.perform(get("/api/admin/services").param("brandId", brand.getId().toString()).with(user(admin)))
                .andExpect(jsonPath("$.content[0].name").value("Toggle Product"))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    void anActivatedProductIsShownAgain() throws Exception {
        setActive("services", product.getId(), false).andExpect(status().isOk());
        setActive("services", product.getId(), true).andExpect(status().isOk());

        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$[*].name", hasItem("Toggle Product")));
    }

    @Test
    void theProductsOfAnInactiveBrandAreHidden() throws Exception {
        setActive("brands", brand.getId(), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/brands"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Toggle Brand"))));
        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Toggle Product"))));
        mockMvc.perform(get("/api/services").param("featured", "true"))
                .andExpect(jsonPath("$[*].name", not(hasItem("Toggle Product"))));
    }

    @Test
    void anInactiveClientIsHiddenFromTheWebsite() throws Exception {
        setActive("clients", client.getId(), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));

        mockMvc.perform(get("/api/clients").param("size", "50"))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Toggle Client"))));
    }

    @Test
    void returns404ForAnUnknownProduct() throws Exception {
        setActive("services", 999999L, false).andExpect(status().isNotFound());
    }
}
