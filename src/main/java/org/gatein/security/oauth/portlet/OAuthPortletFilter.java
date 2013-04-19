/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.security.oauth.portlet;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.common.AccessTokenContext;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.data.SocialNetworkService;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.registry.OAuthProviderTypeRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthPortletFilter implements RenderFilter {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String ATTRIBUTE_ACCESS_TOKEN = "_attrAccessToken";
    public static final String INIT_PARAM_ACCESS_TOKEN_VALIDATION = "accessTokenValidation";
    public static final String ATTRIBUTE_ERROR_MESSAGE = "errorMessage";
    public static final String ATTRIBUTE_OAUTH_PROVIDER_TYPE = "oauthProviderType";

    private enum AccessTokenValidation {
        SKIP, SESSION, ALWAYS
    }

    private SocialNetworkService socialNetworkService;
    private OAuthProviderTypeRegistry oauthProviderTypeRegistry;
    private FilterConfig filterConfig;
    private AccessTokenValidation accessTokenValidation;

    @Override
    public void init(FilterConfig filterConfig) throws PortletException {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        this.socialNetworkService = (SocialNetworkService)container.getComponentInstanceOfType(SocialNetworkService.class);
        this.oauthProviderTypeRegistry = (OAuthProviderTypeRegistry)container.getComponentInstanceOfType(OAuthProviderTypeRegistry.class);

        this.filterConfig = filterConfig;
        String accessTokenValidation = filterConfig.getInitParameter(INIT_PARAM_ACCESS_TOKEN_VALIDATION);
        if (AccessTokenValidation.ALWAYS.name().equals(accessTokenValidation)) {
            this.accessTokenValidation = AccessTokenValidation.ALWAYS;
        } else if (AccessTokenValidation.SKIP.name().equals(accessTokenValidation)) {
            this.accessTokenValidation = AccessTokenValidation.SKIP;
        } else {
            // SESSION is default validation type
            this.accessTokenValidation = AccessTokenValidation.SESSION;
        }

    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(RenderRequest request, RenderResponse response, FilterChain chain) throws IOException, PortletException {
        boolean trace = log.isTraceEnabled();
        if (trace) {
            log.trace("Invoked  doFilter");
        }

        String username = request.getRemoteUser();

        if (username == null) {
            PortletRequestDispatcher prd = filterConfig.getPortletContext().getRequestDispatcher("/jsp/error/anonymous.jsp");
            prd.include(request, response);
            return;
        }

        OAuthProviderType<?> oauthProviderType = getOAuthProvider();


        AccessTokenContext accessToken = getAccessTokenOrRedirectToObtainIt(username, oauthProviderType, request, response);
        if (accessToken != null) {
            accessToken = validateAndSaveAccessToken(request, response, oauthProviderType, accessToken);
            if (accessToken != null) {
                if (trace) {
                    log.trace("Invoking handleRender with accessToken " + accessToken);
                }

                chain.doFilter(request, response);

                if (trace) {
                    log.trace("Finished handleRender");
                }
            }
        }
    }

    protected AccessTokenContext getAccessTokenOrRedirectToObtainIt(String username, OAuthProviderType<?> oauthProviderType, RenderRequest request, RenderResponse response)
            throws IOException, PortletException {
        AccessTokenContext accessToken = socialNetworkService.getOAuthAccessToken(oauthProviderType, username);

        if (accessToken == null) {
            // Will be processed by method actionRedirectToOAuthFlow
            PortletURL actionURL = response.createActionURL();
            actionURL.setParameter(ActionRequest.ACTION_NAME, AbstractSocialPortlet.ACTION_OAUTH_REDIRECT);

            request.setAttribute(ATTRIBUTE_ERROR_MESSAGE, oauthProviderType.getFriendlyName() + " access token not available for you.");
            request.setAttribute(ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
            PortletRequestDispatcher prd = filterConfig.getPortletContext().getRequestDispatcher("/jsp/error/token.jsp");
            prd.include(request, response);
        }

        return accessToken;
    }

    protected AccessTokenContext validateAndSaveAccessToken(PortletRequest request, PortletResponse response, OAuthProviderType<?> oauthProviderType, AccessTokenContext accessToken) throws PortletException, IOException {
        AccessTokenContext previousAccessToken = getAccessToken(request, response);

        if (isValidationNeeded(accessToken, previousAccessToken)) {
            // Validate accessToken
            try {
                accessToken = ((OAuthProviderType)getOAuthProvider()).getOauthProviderProcessor().validateTokenAndUpdateScopes(accessToken);
            } catch (OAuthException oe) {
                String jspPage;
                if (oe.getExceptionCode() == OAuthExceptionCode.EXCEPTION_CODE_ACCESS_TOKEN_ERROR) {
                    request.setAttribute(ATTRIBUTE_ERROR_MESSAGE, oauthProviderType.getFriendlyName() + " access token is invalid.");
                    request.setAttribute(ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
                    jspPage = "/jsp/error/token.jsp";
                } else if (oe.getExceptionCode() == OAuthExceptionCode.EXCEPTION_CODE_UNSPECIFIED_IO_ERROR) {
                    log.error(oe);
                    jspPage = "/jsp/error/io.jsp";
                } else {
                    // Some unexpected error
                    throw new PortletException(oe);
                }

                PortletRequestDispatcher prd = filterConfig.getPortletContext().getRequestDispatcher(jspPage);
                prd.include(request, response);
                return null;
            }
        }

        if (!accessToken.equals(previousAccessToken)) {
            saveAccessToken(request, response, accessToken);
        }

        return accessToken;
    }

    protected OAuthProviderType<?> getOAuthProvider() {
        return oauthProviderTypeRegistry.getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }

    protected AccessTokenContext getAccessToken(PortletRequest req, PortletResponse res) {
        return (AccessTokenContext)req.getPortletSession().getAttribute(ATTRIBUTE_ACCESS_TOKEN);
    }

    protected void saveAccessToken(PortletRequest req, PortletResponse res, AccessTokenContext accessToken) {
        req.getPortletSession().setAttribute(ATTRIBUTE_ACCESS_TOKEN, accessToken);
    }

    private boolean isValidationNeeded(AccessTokenContext accessToken, AccessTokenContext previousAccessToken) {
        if (accessTokenValidation == AccessTokenValidation.ALWAYS) {
            return true;
        } else if (accessTokenValidation == AccessTokenValidation.SKIP) {
            return false;
        } else {
            return !accessToken.equals(previousAccessToken);
        }
    }
}
