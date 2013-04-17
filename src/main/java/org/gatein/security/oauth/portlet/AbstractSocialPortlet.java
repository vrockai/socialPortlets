package org.gatein.security.oauth.portlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ProcessAction;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.webui.util.Util;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.data.SocialNetworkService;
import org.gatein.security.oauth.registry.OAuthProviderTypeRegistry;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractSocialPortlet<T> extends GenericPortlet {

    public static final String ACTION_OAUTH_REDIRECT = "actionOAuthRedirect";

    protected final Logger log = LoggerFactory.getLogger(getClass());

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

        log.debug("PortalName from configuration: " + portalName);
        afterInit(container);
    }


    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException,
            java.io.IOException {
        // Just add some logging for now
        String action = request.getParameter(ActionRequest.ACTION_NAME);
        boolean trace = log.isTraceEnabled();
        if (trace) {
            log.trace("Invoked  processAction with action: " + action);
        }

        super.processAction(request, response);

        if (trace) {
            log.trace("Finished  processAction with action: " + action);
        }
    }

    @ProcessAction(name = ACTION_OAUTH_REDIRECT)
    public void actionRedirectToOAuthFlow(ActionRequest aReq, ActionResponse aResp) throws IOException {
        // Save session attribute with URL to redirect. It will be used by GateIn to return to the page with this portlet after finish OAuth flow
        HttpServletRequest servletReq = getServletRequest();
        HttpSession session = servletReq.getSession();
        session.setAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT, servletReq.getRequestURI());

        // Redirect to start OAuth2 flow
        String reqContextPath = servletReq.getContextPath();
        OAuthProviderType<T> oauthProviderType = getOAuthProvider();
        String initOauthFlowURL = oauthProviderType.getInitOAuthURL(reqContextPath);

        // Attach custom scope
        String customScope = aReq.getParameter(OAuthConstants.PARAM_CUSTOM_SCOPE);
        if (customScope != null) {
            initOauthFlowURL = initOauthFlowURL + "&" + OAuthConstants.PARAM_CUSTOM_SCOPE + "=" + customScope;
        }

        getServletResponse().sendRedirect(initOauthFlowURL);
    }


    // Normally it shouldn't be needed to override this method in subclasses
    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        boolean trace = log.isTraceEnabled();
        if (trace) {
            log.trace("Invoked  doView");
        }

        String username = request.getRemoteUser();

        if (username == null) {
            //writeAndFinishResponse("No content available for anonymous user. You need to login first", response);
            PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/jsp/error/anonymous.jsp");
            prd.include(request, response);
            return;
        }

        OAuthProviderType<T> oauthProviderType = getOAuthProvider();


        T accessToken = getAccessTokenOrRedirectToObtainIt(username, oauthProviderType, request, response);
        if (accessToken != null) {
            if (trace) {
                log.trace("Invoking handleRender with accessToken " + accessToken);
            }
            handleRender(request, response, accessToken);
            if (trace) {
                log.trace("Finished handleRender");
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


    // Intended to be used and/or overriden by subclasses if needed
    protected HttpServletRequest getServletRequest() {
        return Util.getPortalRequestContext().getRequest();
    }


    // Intended to be used and/or overriden by subclasses if needed
    protected HttpServletResponse getServletResponse() {
        return Util.getPortalRequestContext().getResponse();
    }

    // Helper method. Intended to be used by subclasses
    protected String getParameterAndSaveItToSession(String paramName, PortletRequest req, PortletSession session) {
        String paramValue = req.getParameter(paramName);
        if (paramValue != null) {
            session.setAttribute(paramName, paramValue);
        } else {
            paramValue = (String)session.getAttribute(paramName);
        }

        return paramValue;
    }


    private T getAccessTokenOrRedirectToObtainIt(String username, OAuthProviderType<T> oauthProviderType, RenderRequest request, RenderResponse response)
            throws IOException, PortletException {
        T accessToken = socialNetworkService.getOAuthAccessToken(oauthProviderType, username);

        if (accessToken == null) {
            // Will be processed by method actionRedirectToOAuthFlow
            PortletURL actionURL = response.createActionURL();
            actionURL.setParameter(ActionRequest.ACTION_NAME, ACTION_OAUTH_REDIRECT);

            PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/jsp/error/token.jsp");
            prd.include(request, response);
            /*
            writeAndFinishResponse(oauthProviderType.getFriendlyName() + " accessToken not available for you. Click <a href=\"" +
                    actionURL + "\" style=\"color: blue;\">here</a> to link your " + portalName + " account with " + oauthProviderType.getFriendlyName() + " account", response);
                    */
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
