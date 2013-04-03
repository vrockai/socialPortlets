package org.gatein.security.oauth.portlet;

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
import org.gatein.security.oauth.facebook.GateInFacebookProcessor;
import org.gatein.security.oauth.social.FacebookPrincipal;

/**
 * Very simple portlet for displaying basic information about Facebook user
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookPortlet extends AbstractSocialPortlet<String> {

    private GateInFacebookProcessor gtnFacebookProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.gtnFacebookProcessor = (GateInFacebookProcessor)container.getComponentInstanceOfType(GateInFacebookProcessor.class);
    }

    @Override
    protected OAuthProviderType<String> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }

    @Override
    protected void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, String accessToken) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, String accessToken) throws IOException {
        FacebookPrincipal principal = gtnFacebookProcessor.getPrincipal(accessToken);
        writeAndFinishResponse(principal.toString(), response);
    }
}
