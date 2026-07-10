package com.orientsec.genesis.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class LogoutResponseSupportTest {

    @Test
    void clearsAuthenticationCookiesBeforeRedirecting() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        LogoutResponseSupport.clearCookiesAndRedirect(
                response, "BJCA_TOKEN", "https://portal.example.test/");

        assertThat(response.getRedirectedUrl()).isEqualTo("https://portal.example.test/");
        assertThat(response.getHeaders("Set-Cookie"))
                .hasSize(2)
                .allSatisfy(
                        header ->
                                assertThat(header)
                                        .contains(
                                                "Path=/",
                                                "Max-Age=0",
                                                "HttpOnly",
                                                "SameSite=Lax",
                                                "Secure"));
        assertThat(response.getHeaders("Set-Cookie").get(0)).startsWith("BJCA_TOKEN=");
        assertThat(response.getHeaders("Set-Cookie").get(1)).startsWith("gns_session=");
    }

    @Test
    void rejectsMissingRedirectTarget() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(
                        () ->
                                LogoutResponseSupport.clearCookiesAndRedirect(
                                        response, "BJCA_TOKEN", " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ca.domainName");
    }

    @Test
    void rejectsNonHttpRedirectTarget() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(
                        () ->
                                LogoutResponseSupport.clearCookiesAndRedirect(
                                        response, "BJCA_TOKEN", "javascript:alert(1)"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP(S)");
    }
}
