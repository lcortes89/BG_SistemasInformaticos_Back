package com.bgsistemas.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// BGSI-274: the filter in front of POST /api/auth/login
class LoginAttemptFilterTest {

    private final LoginAttemptService service = new LoginAttemptService();
    private final LoginAttemptFilter filter = new LoginAttemptFilter(service);

    private static String basic(String email, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpServletRequest login(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LoginAttemptFilter.LOGIN_PATH);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    // A fake rest of the chain that answers the given status, like Spring Security would
    private FilterChain answering(int status) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doAnswer(call -> {
            ((HttpServletResponse) call.getArgument(1)).setStatus(status);
            return null;
        }).when(chain).doFilter(any(), any());
        return chain;
    }

    @Test
    void countsTheWrongPasswordsAndAnswers429WhenLocked() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            filter.doFilter(login(basic("admin@mail.com", "wrong")), new MockHttpServletResponse(), answering(401));
        }

        FilterChain chain = answering(200);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(login(basic("admin@mail.com", "Correct2026")), response, chain);

        verify(chain, never()).doFilter(any(), any()); // the password is not even checked
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotBlank();
        assertThat(response.getContentAsString()).contains("Try again in 15 minutes").contains("\"retryAfterMinutes\":\"15\"");
    }

    @Test
    void aCorrectLoginResetsTheCount() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            filter.doFilter(login(basic("admin@mail.com", "wrong")), new MockHttpServletResponse(), answering(401));
        }
        filter.doFilter(login(basic("admin@mail.com", "Correct2026")), new MockHttpServletResponse(), answering(200));
        filter.doFilter(login(basic("admin@mail.com", "wrong")), new MockHttpServletResponse(), answering(401));

        assertThat(service.lockRemaining("admin@mail.com", Instant.now())).isEmpty();
    }

    @Test
    void doesNotCountOtherAnswers() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            filter.doFilter(login(basic("admin@mail.com", "x")), new MockHttpServletResponse(), answering(500));
        }

        assertThat(service.lockRemaining("admin@mail.com", Instant.now())).isEmpty();
    }

    @Test
    void letsThroughRequestsWithoutBasicCredentials() throws Exception {
        for (String header : new String[] {null, "Bearer abc", "Basic not-base64!", basic("", "x")}) {
            FilterChain chain = answering(401);
            filter.doFilter(login(header), new MockHttpServletResponse(), chain);
            verify(chain).doFilter(any(), any());
        }
        assertThat(service.trackedEmails()).isZero();
    }

    @Test
    void onlyWatchesThePostToTheLoginPath() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/auth/login"))).isFalse();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/auth/login"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/contacts"))).isTrue();
    }

    @Test
    void readsTheEmailOfTheBasicHeader() {
        assertThat(LoginAttemptFilter.basicEmail(basic("admin@mail.com", "pa:ss"))).contains("admin@mail.com");
        assertThat(LoginAttemptFilter.basicEmail("basic " + Base64.getEncoder().encodeToString("a@b.c:x".getBytes()))).contains("a@b.c");
        assertThat(LoginAttemptFilter.basicEmail("Basic " + Base64.getEncoder().encodeToString("no-colon".getBytes()))).isEmpty();
    }
}
