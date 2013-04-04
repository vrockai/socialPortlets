package org.gatein.security.oauth.portlet.google;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.ActivityFeed;
import com.google.api.services.plus.model.Comment;
import com.google.api.services.plus.model.CommentFeed;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.google.GoogleProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleActivitiesPortlet extends AbstractSocialPortlet<GoogleTokenResponse> {

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
        PrintWriter writer = response.getWriter();

        // See https://developers.google.com/+/api/latest/activities/list for details
        Plus service = googleProcessor.getPlusService(accessToken);
        final Plus.Activities.List list  = service.activities().list("me", "public");
        list.setMaxResults(10L);

        ActivityFeed activityFeed = new GoogleRequest<ActivityFeed>(response, "https://www.googleapis.com/auth/plus.login") {

            ActivityFeed run() throws IOException {
                return list.execute();
            }

        }.sendRequest();

        if (activityFeed != null) {
            writer.println("<h2>Your last google+ activities</h2>");
            for (Activity activity : activityFeed.getItems()) {
                Activity.PlusObject activityObject = activity.getObject();
                writer.println("<h3>" + activity.getTitle() + "</h3>");
                writer.println("Likes: <b>" + activityObject.getPlusoners().getTotalItems() + "</b>");
                writer.println(", Resharers: <b>" + activityObject.getResharers().getTotalItems() + "</b>, ");
                writer.println("<a href=\"" + activity.getUrl() + "\" style=\"color: blue;\">Activity details</a><br><br>");

                CommentFeed comments = service.comments().list(activity.getId()).execute();

                int counter = 1;
                for (Comment comment : comments.getItems()) {
                    writer.println("<b>Comment " + counter + "</b><br>");
                    writer.println("From: " + comment.getActor().getDisplayName() + "<br>");
                    writer.println("Text: " + comment.getObject().getContent() + "<br>");
                    writer.println("Likes: " + comment.getPlusoners().getTotalItems() + "<br><br>");
                    counter++;
                }

                writer.println("<hr>");
            }
        }
    }
}
