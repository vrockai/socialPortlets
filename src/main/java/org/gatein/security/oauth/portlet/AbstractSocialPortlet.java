package org.gatein.security.oauth.portlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.ProcessAction;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.data.SocialNetworkService;
import org.gatein.security.oauth.registry.OAuthProviderTypeRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractSocialPortlet<T> extends GenericPortlet {

    public static final String ACTION_OAUTH_REDIRECT = "actionOAuthRedirect";

    public static final String RENDER_ACTION = "renderAction";
    public static final String RENDER_PARAM_OAUTH_REDIRECT = "renderParamOAuthRedirect";

    private SocialNetworkService socialNetworkService;
    private OAuthProviderTypeRegistry oauthProviderTypeRegistry;
    private String portalName; // Difference between GateIn/JPP


    @Override
    public final void init() throws PortletException {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        this.socialNetworkService = (SocialNetworkService)container.getComponentInstanceOfType(SocialNetworkService.class);
        this.oauthProviderTypeRegistry = (OAuthProviderTypeRegistry)container.getComponentInstanceOfType(OAuthProviderTypeRegistry.class);
        this.portalName = getPortletConfig().getInitParameter("portalName");
        if (this.portalName == null) {
            this.portalName = "GateIn";
        }

        afterInit(container);
    }


    @ProcessAction(name = ACTION_OAUTH_REDIRECT)
    public void actionRedirectToOAuthFlow(ActionRequest aReq, ActionResponse aResp) {
        // Save session attribute with URL to redirect. It will be used by GateIn to return to the page with this portlet after finish OAuth flow
        HttpServletRequest servletReq = getServletRequest();
        HttpSession session = servletReq.getSession();
        session.setAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT, servletReq.getRequestURI());

        aReq.getPortletSession().setAttribute(RENDER_ACTION, RENDER_PARAM_OAUTH_REDIRECT);
    }


    // Normally it shouldn't be needed to override this method in subclasses
    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String username = request.getRemoteUser();

        if (username == null) {
            writeAndFinishResponse("No content available for anonymous user. You need to login first", response);
            return;
        }

        OAuthProviderType<T> oauthProviderType = getOAuthProvider();

        String renderAction = (String)request.getPortletSession().getAttribute(RENDER_ACTION);
        if (renderAction != null) {
            if (RENDER_PARAM_OAUTH_REDIRECT.equals(renderAction)) {

                // Method actionRedirectToOAuthFlow was finished. We need to redirect to GateIn OAuth flow
                String reqContextPath = getServletRequest().getContextPath();
                String initOauthFlowURL = oauthProviderType.getInitOAuthURL(reqContextPath);
                getServletResponse().sendRedirect(initOauthFlowURL);

                // Clear state in session
                request.getPortletSession().removeAttribute(RENDER_ACTION);
            } else {

                T accessToken = getAccessTokenOrRedirectToObtainIt(username, oauthProviderType, response);
                if (accessToken != null) {
                    handleRenderAction(request, response, renderAction, accessToken);
                }
            }
        } else {

            T accessToken = getAccessTokenOrRedirectToObtainIt(username, oauthProviderType, response);
            if (accessToken != null) {
                handleRender(request, response, accessToken);
            }
        }
    }


    // Intended to be used by subclasses
    protected final void writeAndFinishResponse(String content, RenderResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println(content);
        writer.close();
    }


    // Intended to be used by subclasses
    protected final SocialNetworkService getSocialNetworkService() {
        return socialNetworkService;
    }


    // Intended to be used by subclasses
    protected OAuthProviderTypeRegistry getOauthProviderTypeRegistry() {
        return oauthProviderTypeRegistry;
    }

    // Intended to be used by subclasses
    protected final String getPortalName() {
        return portalName;
    }


    // Intended to be overriden by subclasses if needed
    protected HttpServletRequest getServletRequest() {
        return Util.getPortalRequestContext().getRequest();
    }


    // Intended to be overriden by subclasses if needed
    protected HttpServletResponse getServletResponse() {
        return Util.getPortalRequestContext().getResponse();
    }


    private T getAccessTokenOrRedirectToObtainIt(String username, OAuthProviderType<T> oauthProviderType, RenderResponse response)
            throws IOException {
        T accessToken = socialNetworkService.getOAuthAccessToken(oauthProviderType, username);

        if (accessToken == null) {
            // Will be processed by method actionRedirectToOAuthFlow
            PortletURL actionURL = response.createActionURL();
            actionURL.setParameter(ActionRequest.ACTION_NAME, ACTION_OAUTH_REDIRECT);

            writeAndFinishResponse(oauthProviderType.getFriendlyName() + " accessToken not available for you. Click <a href=\"" +
                    actionURL + "\" style=\"color: blue;\">here</a> to link your " + portalName + " account with " + oauthProviderType.getFriendlyName() + " account", response);
        }

        return accessToken;
    }


    /**
     * Subclass should perform it's own initialization and obtain needed kernel services
     *
     * @param container current {@link ExoContainer} which should be used to obtain needed kernel services
     */
    protected abstract void afterInit(ExoContainer container);


    /**
     * Subclass should provide concrete instance of {@link OAuthProviderType}
     *
     * @return instance of OAuth provider
     */
    protected abstract OAuthProviderType<T> getOAuthProvider();


    /**
     * Used to handle rendering when some portlet action was performed and this action set value to parameter {@link #RENDER_ACTION}.
     * AccessToken is available via parameter, so subclass is able to perform some calls to OAuth Provider (social network) and
     * do some interesting stuff with it.
     *
     * @param request render request
     * @param response render response
     * @param renderAction value of renderParameter. Subclass should decide which render flow it needs to perform based on this value
     * @param accessToken non-null accessToken, which could be used to perform operations in given OAuth provider (Social network)
     */
    protected abstract void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, T accessToken)
            throws PortletException, IOException;


    /**
     * Used to handle rendering. AccessToken is available via parameter, so subclass is able to perform some calls to OAuth
     * Provider (social network) and do some interesting stuff with it.
     *
     * @param request render request
     * @param response render response
     * @param accessToken non-null accessToken, which could be used to perform operations in given OAuth provider (Social network)
     */
    protected abstract void handleRender(RenderRequest request, RenderResponse response, T accessToken)
            throws PortletException, IOException;
}
