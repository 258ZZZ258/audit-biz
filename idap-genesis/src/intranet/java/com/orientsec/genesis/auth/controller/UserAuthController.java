package com.orientsec.genesis.auth.controller;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.auth.filter.bjca.filter.GenesisFilter;
import com.orientsec.genesis.auth.filter.bjca.utils.BJCAUtils;
import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.genesis.auth.util.GenesisSessionUtils;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Genesis current-user, menu and logout endpoints from the intranet application baseline. */
@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(name = "genesis.auth.enabled", havingValue = "true")
public class UserAuthController {

    private static final Logger log = LoggerFactory.getLogger(UserAuthController.class);

    private final GenesisUserService genesisUserService;
    private final BJCAUtils tokenUtils;
    private final String domainName;

    public UserAuthController(
            GenesisUserService genesisUserService,
            BJCAUtils tokenUtils,
            @Value("${ca.domainName:}") String domainName) {
        this.genesisUserService = genesisUserService;
        this.tokenUtils = tokenUtils;
        this.domainName = domainName;
    }

    @GetMapping("/menu_list")
    public Result<List<Menu>> menuList() {
        return ResultGenerator.genSuccessResult(genesisUserService.getUserMenus());
    }

    @GetMapping("/whoami")
    public Result<User> whoAmI() {
        return ResultGenerator.genSuccessResult(genesisUserService.getCurrentUser());
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            logoutGenesisSession(request.getRequestedSessionId());
            LogoutResponseSupport.clearCookiesAndRedirect(
                    response, BJCAUtils.TOKEN_COOKIE, domainName);
        } catch (Exception e) {
            log.warn("Genesis 登出失败({})", e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void logoutGenesisSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        Subject subject = GenesisSessionUtils.getSubjectBySessionId(sessionId);
        if (subject == null) {
            return;
        }
        Session session = subject.getSession(false);
        Object tokenId = session == null ? null : session.getAttribute(GenesisFilter.TOKENID);
        if (tokenId instanceof String && StringUtils.hasText((String) tokenId)) {
            try {
                tokenUtils.loginOut((String) tokenId);
            } finally {
                subject.logout();
            }
            return;
        }
        subject.logout();
    }
}
