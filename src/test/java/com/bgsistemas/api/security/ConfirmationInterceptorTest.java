package com.bgsistemas.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// BGSI-275: only methods with @RequiresConfirmation need a valid X-Confirm-Token
class ConfirmationInterceptorTest {

    static class FakeController {
        @RequiresConfirmation
        public void delete() {
        }

        public void edit() {
        }
    }

    private final ConfirmationTokenService tokens = new ConfirmationTokenService();
    private final ConfirmationInterceptor interceptor = new ConfirmationInterceptor(tokens);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new FakeController(), FakeController.class.getMethod(method));
    }

    private void logIn(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void actionsWithoutTheAnnotationDoNotNeedTheConfirmation() throws Exception {
        logIn("admin@mail.com");

        assertThat(interceptor.preHandle(request, response, handler("edit"))).isTrue();
    }

    @Test
    void aSensitiveActionWithoutTheHeaderIsRejected() throws Exception {
        logIn("admin@mail.com");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("delete")))
                .isInstanceOf(ConfirmationRequiredException.class);
    }

    @Test
    void aSensitiveActionWithAValidTokenGoesOn() throws Exception {
        logIn("admin@mail.com");
        request.addHeader(ConfirmationInterceptor.HEADER, tokens.issue("admin@mail.com", Instant.now()));

        assertThat(interceptor.preHandle(request, response, handler("delete"))).isTrue();
    }

    @Test
    void theTokenOfAnotherAdminIsRejected() throws Exception {
        logIn("admin@mail.com");
        request.addHeader(ConfirmationInterceptor.HEADER, tokens.issue("other@mail.com", Instant.now()));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("delete")))
                .isInstanceOf(ConfirmationRequiredException.class);
    }

    @Test
    void handlersThatAreNotControllerMethodsGoOn() throws Exception {
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }
}
