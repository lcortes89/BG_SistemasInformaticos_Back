package com.bgsistemas.api.contact;

import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-307: delete a contact with the H2 database. It needs the password typed again (BGSI-275)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ContactDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SecurityUser saveAdmin(String email) {
        return new SecurityUser(userRepository.save(new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN)));
    }

    private String confirmToken(SecurityUser admin) throws Exception {
        String json = mockMvc.perform(post("/api/admin/account/confirm").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Correct2026\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.confirmToken");
    }

    private Contact saveContact() {
        return contactRepository.save(new Contact("Spam Bot", "spam@test.com", "600000000", "BDP One", "Buy now", true));
    }

    @Test
    void deletesTheContactWithThePasswordConfirmed() throws Exception {
        SecurityUser admin = saveAdmin("delete-contact@test.com");
        Contact contact = saveContact();

        mockMvc.perform(delete("/api/admin/contacts/" + contact.getId()).with(user(admin))
                        .header(ConfirmationInterceptor.HEADER, confirmToken(admin)))
                .andExpect(status().isNoContent());

        assertThat(contactRepository.findById(contact.getId())).isEmpty();
    }

    @Test
    void withoutThePasswordTheContactIsKept() throws Exception {
        SecurityUser admin = saveAdmin("no-confirm-contact@test.com");
        Contact contact = saveContact();

        mockMvc.perform(delete("/api/admin/contacts/" + contact.getId()).with(user(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.reason").value("CONFIRMATION_REQUIRED"));

        assertThat(contactRepository.findById(contact.getId())).isPresent();
    }

    @Test
    void aContactThatDoesNotExistGives404() throws Exception {
        SecurityUser admin = saveAdmin("missing-contact@test.com");

        mockMvc.perform(delete("/api/admin/contacts/999999").with(user(admin))
                        .header(ConfirmationInterceptor.HEADER, confirmToken(admin)))
                .andExpect(status().isNotFound());
    }
}
