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
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookPortlet extends GenericPortlet {

    private SocialNetworkService socialNetworkService;
    private GateInFacebookProcessor gtnFacebookProcessor;

    @Override
    public void init() throws PortletException {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        this.socialNetworkService = (SocialNetworkService)container.getComponentInstanceOfType(SocialNetworkService.class);
        this.gtnFacebookProcessor = (GateInFacebookProcessor)container.getComponentInstanceOfType(GateInFacebookProcessor.class);
    }

    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws IOException {
        String username = request.getRemoteUser();

        if (username == null) {
            writeAndFinishResponse("No content available for anonymous user. You need to login first", response);
            return;
        }

        String accessToken = socialNetworkService.getOAuthAccessToken(OAuthProviderType.FACEBOOK, username);

        if (accessToken == null) {
            // Save attr with URL to redirect
            // TODO: It's saved immediately but it should be saved after click to link
            HttpServletRequest servletReq = getServletRequest();
            HttpSession session = servletReq.getSession();
            session.setAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT, servletReq.getRequestURI());

            writeAndFinishResponse("Facebook accessToken not available for you. Click <a href=\"" +
                    getFacebookAuthRedirectLink() + "\">here</a> to link your GateIn account with Facebook account", response);
            return;
        }

        FacebookPrincipal principal = gtnFacebookProcessor.getPrincipal(accessToken);
        writeAndFinishResponse(principal.toString() + "<hr>AccessToken of Facebook principal: " + principal.getAccessToken(), response);
    }

    private void writeAndFinishResponse(String content, RenderResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println(content);
        writer.close();
    }

    private String getFacebookAuthRedirectLink() {
        String reqContextPath = getServletRequest().getContextPath();
        return OAuthProviderType.FACEBOOK.getInitOAuthURL(reqContextPath);
    }

    // TODO: Is it better way? Maybe PolicyContext but this works only in JBoss env...
    protected HttpServletRequest getServletRequest() {
        return Util.getPortalRequestContext().getRequest();
    }
}
