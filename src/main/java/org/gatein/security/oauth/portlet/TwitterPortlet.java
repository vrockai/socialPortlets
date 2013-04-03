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
public class TwitterPortlet extends AbstractSocialPortlet<TwitterAccessTokenContext> {

    private TwitterProcessor gtnTwitterProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.gtnTwitterProcessor = (TwitterProcessor) container.getComponentInstanceOfType(TwitterProcessor.class);
    }

    @Override
    protected OAuthProviderType<TwitterAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_TWITTER);
    }

    @Override
    protected void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, TwitterAccessTokenContext accessToken) throws PortletException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, TwitterAccessTokenContext accessToken) throws PortletException, IOException {
        Twitter twitter = gtnTwitterProcessor.getAuthorizedTwitterInstance(accessToken);

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

//        htmlResponse.append("<hr>");
//
//        htmlResponse.append("Access Token: " + accessToken.getAccessToken() + "<br>");
//        htmlResponse.append("Access Token Secret: " + accessToken.getAccessTokenSecret() + "<br>");

        writeAndFinishResponse(htmlResponse.toString(), response);
    }
}
