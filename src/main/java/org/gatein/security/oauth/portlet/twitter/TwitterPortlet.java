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


package org.gatein.security.oauth.portlet.twitter;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;
import org.gatein.security.oauth.portlet.OAuthPortletFilter;
import org.gatein.security.oauth.twitter.TwitterAccessTokenContext;
import org.gatein.security.oauth.twitter.TwitterProcessor;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TwitterPortlet extends AbstractSocialPortlet<TwitterAccessTokenContext> {

    private TwitterProcessor gtnTwitterProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.gtnTwitterProcessor = (TwitterProcessor) container.getComponentInstanceOfType(TwitterProcessor.class);
    }

    @Override
    protected OAuthProviderType<TwitterAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_TWITTER);
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, TwitterAccessTokenContext accessToken) throws PortletException, IOException {
        Twitter twitter = gtnTwitterProcessor.getAuthorizedTwitterInstance(accessToken);

        User twitterUser = null;
        try {
            twitterUser = twitter.verifyCredentials();
        } catch (TwitterException te) {
            handleException(request, response, te);
        }

        if (twitterUser != null) {
            request.setAttribute("twitterUserInfo", twitterUser);
            PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/jsp/twitter/userinfo.jsp");
            prd.include(request, response);
        }
    }

    private void handleException(RenderRequest request, RenderResponse response, TwitterException te) throws PortletException, IOException {
        OAuthProviderType<TwitterAccessTokenContext> oauthProviderType = getOAuthProvider();

        String jspErrorPage;
        if (te.getStatusCode() == 401) {
            request.setAttribute(OAuthPortletFilter.ATTRIBUTE_ERROR_MESSAGE, oauthProviderType.getFriendlyName() + " access token is invalid.");
            request.setAttribute(OAuthPortletFilter.ATTRIBUTE_OAUTH_PROVIDER_TYPE, oauthProviderType);
            jspErrorPage = "/jsp/error/token.jsp";
        } else {
            log.error(te);
            jspErrorPage = "/jsp/error/io.jsp";
        }

        PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher(jspErrorPage);
        prd.include(request, response);
    }
}
