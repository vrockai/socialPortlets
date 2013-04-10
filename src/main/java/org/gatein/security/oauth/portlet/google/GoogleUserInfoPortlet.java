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

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.google.GoogleProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleUserInfoPortlet extends AbstractSocialPortlet<GoogleTokenResponse> {

    private GoogleProcessor googleProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.googleProcessor = (GoogleProcessor)container.getComponentInstanceOfType(GoogleProcessor.class);
    }

    @Override
    protected OAuthProviderType<GoogleTokenResponse> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_GOOGLE);
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, GoogleTokenResponse accessToken) throws PortletException, IOException {
        final Oauth2 oauth2 = googleProcessor.getOAuth2Instance(accessToken);

        Userinfo uinfo = new GoogleRequest<Userinfo>(response, "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile") {

            @Override
            Userinfo run() throws IOException {
                return oauth2.userinfo().v2().me().get().execute();
            }

        }.sendRequest();

        if (uinfo != null) {
            StringBuilder builder = new StringBuilder("Given name: " + uinfo.getGivenName())
                    .append("<br>Family name: " + uinfo.getFamilyName())
                    .append("<br>Email: " + uinfo.getEmail())
                    .append("<br>Birthday: " + uinfo.getBirthday())
                    .append("<br>Gender: " + uinfo.getGender())
                    .append("<br>Locale: " + uinfo.getLocale())
                    .append("<br><img src=\"" + uinfo.getPicture() + "?size=100\" title=\"" + uinfo.getName() + "\" />");
            writeAndFinishResponse(builder.toString(), response);
        }
    }
}
