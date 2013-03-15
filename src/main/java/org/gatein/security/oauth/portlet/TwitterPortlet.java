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
import org.gatein.security.oauth.twitter.TwitterAccessTokenContext;
import org.gatein.security.oauth.twitter.TwitterProcessor;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TwitterPortlet extends GenericPortlet {

    private SocialNetworkService socialNetworkService;
    private TwitterProcessor gtnTwitterProcessor;

    @Override
    public void init() throws PortletException {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        this.socialNetworkService = (SocialNetworkService) container.getComponentInstanceOfType(SocialNetworkService.class);
        this.gtnTwitterProcessor = (TwitterProcessor) container.getComponentInstanceOfType(TwitterProcessor.class);
    }

    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String username = request.getRemoteUser();

        if (username == null) {
            writeAndFinishResponse("No content available for anonymous user. You need to login first", response);
            return;
        }

        String accessToken = socialNetworkService.getOAuthAccessToken(OAuthProviderType.TWITTER, username);

        if (accessToken == null) {
            // Save attr with URL to redirect
            // TODO: It's saved immediately but it should be saved after click to link
            HttpServletRequest servletReq = getServletRequest();
            HttpSession session = servletReq.getSession();
            session.setAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT, servletReq.getRequestURI());

            writeAndFinishResponse("Twitter accessToken not available for you. Click <a href=\"" +
                    getTwitterAuthRedirectLink() + "\">here</a> to link your GateIn account with Twitter account", response);
            return;
        }

        TwitterAccessTokenContext accessTokenContext = gtnTwitterProcessor.getAccessTokenFromString(accessToken);
        Twitter twitter = gtnTwitterProcessor.getAuthorizedTwitterInstance(accessTokenContext);

        User twitterUser;
        try {
            twitterUser = twitter.verifyCredentials();
        } catch (TwitterException te) {
            throw new PortletException(te);
        }

        StringBuilder htmlResponse = new StringBuilder("Twitter username: " + twitterUser.getScreenName() + "<br>");
        htmlResponse.append("Twitter name: " + twitterUser.getName() + "<br>");
        htmlResponse.append("Tweets: " + twitterUser.getStatusesCount() + ", Friends: " + twitterUser.getFriendsCount() +
                ", Followers: " + twitterUser.getFollowersCount() + "<br>");
        htmlResponse.append("Last tweet: " + twitterUser.getStatus().getText() + "<br>");
        htmlResponse.append("<img src=\"" + twitterUser.getProfileImageURL() + "\" alt=\"Your picture\" /><br>");

        htmlResponse.append("<hr>");

        htmlResponse.append("Access Token: " + accessTokenContext.getAccessToken() + "<br>");
        htmlResponse.append("Access Token Secret: " + accessTokenContext.getAccessTokenSecret() + "<br>");

        writeAndFinishResponse(htmlResponse.toString(), response);
    }

    private void writeAndFinishResponse(String content, RenderResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println(content);
        writer.close();
    }

    private String getTwitterAuthRedirectLink() {
        String reqContextPath = getServletRequest().getContextPath();
        return OAuthProviderType.TWITTER.getInitOAuthURL(reqContextPath);
    }

    // TODO: Is it better way? Maybe PolicyContext but this works only in JBoss env...
    protected HttpServletRequest getServletRequest() {
        return Util.getPortalRequestContext().getRequest();
    }
}
