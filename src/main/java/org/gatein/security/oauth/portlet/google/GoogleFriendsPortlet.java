package org.gatein.security.oauth.portlet.google;

import java.io.IOException;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleFriendsPortlet extends AbstractSocialPortlet<GoogleTokenResponse> {

    @Override
    protected void afterInit(ExoContainer container) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected OAuthProviderType<GoogleTokenResponse> getOAuthProvider() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, GoogleTokenResponse accessToken) throws PortletException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, GoogleTokenResponse accessToken) throws PortletException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
