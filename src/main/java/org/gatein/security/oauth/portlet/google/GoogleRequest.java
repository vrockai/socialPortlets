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


package org.gatein.security.oauth.portlet.google;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.restfb.exception.FacebookException;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.google.GoogleAccessTokenContext;
import org.gatein.security.oauth.portlet.OAuthPortletFilter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class GoogleRequest<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final RenderRequest request;
    private final RenderResponse response;
    private final PortletContext portletContext;
    private final OAuthProviderType<GoogleAccessTokenContext> oauthProviderType;
    private final String requiredScope;

    GoogleRequest(RenderRequest request, RenderResponse response, PortletContext portletContext,
                  OAuthProviderType<GoogleAccessTokenContext> oauthPrType, String requiredScope) {
        this.request = request;
        this.response = response;
        this.portletContext = portletContext;
        this.oauthProviderType = oauthPrType;
        this.requiredScope = requiredScope;
    }


    protected abstract T execute() throws IOException;


    T sendRequest() throws PortletException, IOException {
        String jspErrorPage;

        try {
            return execute();
        } catch (GoogleJsonResponseException googleEx) {
            String message = oauthProviderType.getFriendlyName() + " access token is invalid or scope is insufficient.";
            if (requiredScope != null) {
                message = message + "You will need scope: " + requiredScope + "<br>";
                request.setAttribute(OAuthConstants.PARAM_CUSTOM_SCOPE, requiredScope);
            }
            request.setAttribute(OAuthPortletFilter.ATTRIBUTE_ERROR_MESSAGE, message);
            request.setAttribute(OAuthPortletFilter.ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
            jspErrorPage = "/jsp/error/token.jsp";
        } catch (IOException ioe) {
            log.error(ioe);
            jspErrorPage = "/jsp/error/io.jsp";
        }

        PortletRequestDispatcher prd = portletContext.getRequestDispatcher(jspErrorPage);
        prd.include(request, response);
        return null;
    }

}
