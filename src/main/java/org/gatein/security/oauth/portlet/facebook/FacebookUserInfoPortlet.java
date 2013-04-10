package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.data.SocialNetworkService;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.facebook.GateInFacebookProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;
import org.gatein.security.oauth.social.FacebookPrincipal;

/**
 * Very simple portlet for displaying basic information about Facebook user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookUserInfoPortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

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
        FacebookPrincipal principal = gtnFacebookProcessor.getPrincipal(accessToken.getAccessToken());
        writeAndFinishResponse(principal.toString(), response);
    }
}
