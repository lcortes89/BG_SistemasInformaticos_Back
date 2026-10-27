package com.bgsistemas.api.upload;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AdminUploadController.class)
class AdminUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private final MockMultipartFile png = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

    @Test
    void shouldReturnTheUrlOfTheSavedImage() throws Exception {
        when(imageStorageService.store(any())).thenReturn("abc123.png");
        // BGSI-327: the storage decides the URL (local folder or Supabase)
        when(imageStorageService.publicUrl("abc123.png"))
                .thenReturn("https://abc.supabase.co/storage/v1/object/public/images/abc123.png");

        mockMvc.perform(multipart("/api/admin/uploads").file(png).with(user(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://abc.supabase.co/storage/v1/object/public/images/abc123.png"));
    }

    @Test
    void shouldReturn400WhenTheImageIsNotValid() throws Exception {
        when(imageStorageService.store(any())).thenThrow(new InvalidImageException("Only JPG, PNG or WebP images are allowed"));

        mockMvc.perform(multipart("/api/admin/uploads").file(png).with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only JPG, PNG or WebP images are allowed"));
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads").file(png))
                .andExpect(status().isUnauthorized());

        verify(imageStorageService, never()).store(any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(multipart("/api/admin/uploads").file(png).with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());

        verify(imageStorageService, never()).store(any());
    }
}
