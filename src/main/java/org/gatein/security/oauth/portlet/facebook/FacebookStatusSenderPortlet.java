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

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.facebook.GateInFacebookProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookStatusSenderPortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

    private GateInFacebookProcessor gtnFacebookProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.gtnFacebookProcessor = (GateInFacebookProcessor)container.getComponentInstanceOfType(GateInFacebookProcessor.class);
    }

    @Override
    protected OAuthProviderType<FacebookAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }


    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, FacebookAccessTokenContext accessToken) throws IOException {

    }
}
