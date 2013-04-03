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
    protected void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, GoogleTokenResponse accessToken) throws PortletException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, GoogleTokenResponse accessToken) throws PortletException, IOException {
        Oauth2 oauth2 = googleProcessor.getOAuth2Instance(accessToken);
        Userinfo uinfo = oauth2.userinfo().v2().me().get().execute();

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
