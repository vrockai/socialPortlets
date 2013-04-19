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

package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.exception.FacebookOAuthException;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.common.AccessTokenContext;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.portlet.OAuthPortletFilter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class FacebookRequest<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final RenderRequest request;
    private final RenderResponse response;
    private final PortletContext portletContext;
    private final OAuthProviderType<FacebookAccessTokenContext> oauthProviderType;

    FacebookRequest(RenderRequest request, RenderResponse response, PortletContext portletContext, OAuthProviderType<FacebookAccessTokenContext> oauthPrType) {
        this.request = request;
        this.response = response;
        this.portletContext = portletContext;
        this.oauthProviderType = oauthPrType;
    }


    protected abstract T execute() throws OAuthException, FacebookException;


    T sendRequest() throws IOException, PortletException {
        try {
            return execute();
        } catch (OAuthException oe) {
            String jspPage;
            if (oe.getExceptionCode() == OAuthExceptionCode.EXCEPTION_CODE_ACCESS_TOKEN_ERROR) {
                request.setAttribute(OAuthPortletFilter.ATTRIBUTE_ERROR_MESSAGE, oauthProviderType.getFriendlyName() + " access token is invalid.");
                request.setAttribute(OAuthPortletFilter.ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
                jspPage = "/jsp/error/token.jsp";
            } else if (oe.getExceptionCode() == OAuthExceptionCode.EXCEPTION_CODE_UNSPECIFIED_IO_ERROR) {
                log.error(oe);
                jspPage = "/jsp/error/io.jsp";
            } else {
                // Do the same like with IO error for now
                log.error(oe);
                jspPage = "/jsp/error/io.jsp";
            }

            PortletRequestDispatcher prd = portletContext.getRequestDispatcher(jspPage);
            prd.include(request, response);
        } catch (FacebookException fe) {
            String jspPage;
            if (fe instanceof FacebookOAuthException) {
                request.setAttribute(OAuthPortletFilter.ATTRIBUTE_ERROR_MESSAGE, oauthProviderType.getFriendlyName() + " access token is invalid.");
                request.setAttribute(OAuthPortletFilter.ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
                jspPage = "/jsp/error/token.jsp";
            } else if (fe instanceof FacebookNetworkException) {
                log.error(fe);
                jspPage = "/jsp/error/io.jsp";
            } else {
                // Do the same like with IO error for now
                log.error(fe);
                jspPage = "/jsp/error/io.jsp";
            }

            PortletRequestDispatcher prd = portletContext.getRequestDispatcher("/jsp/error/token.jsp");
            prd.include(request, response);
        }

        return null;
    }
}
