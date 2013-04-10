/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Comment;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.StatusMessage;
import com.restfb.types.User;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookFriendsPortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

    private static final String ATTR_FRIENDS_COUNT = "friendsCount";
    private static final String PARAM_PAGE = "_page";
    private static final String PARAM_FRIEND_ID = "_friendID";
    private static final int ITEMS_PER_PAGE = 10;

    @Override
    protected void afterInit(ExoContainer container) {
    }

    @Override
    protected OAuthProviderType<FacebookAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }


    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, FacebookAccessTokenContext accessToken) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("handleRender with accessToken: " + accessToken);
        }
        PortletSession session = request.getPortletSession();
        PrintWriter out = response.getWriter();

        FacebookClient facebookClient = new DefaultFacebookClient(accessToken.getAccessToken());
        User user = facebookClient.fetchObject("me", User.class);
        out.println("User: " + user.getName() + "<br>");
        out.println("username: " + user.getUsername() + "<br>");
        out.println("email: " + user.getEmail() + "<br>");
        out.println("<hr>");

        // Count total number of friends
        Integer friendsCount = (Integer)session.getAttribute(ATTR_FRIENDS_COUNT);
        if (friendsCount == null) {
            Connection<NamedFacebookType> myFriends = facebookClient.fetchConnection("me/friends", NamedFacebookType.class);
            friendsCount = myFriends.getData().size();
            session.setAttribute(ATTR_FRIENDS_COUNT, friendsCount);
        }

        Integer pageNumber;
        if (request.getParameter(PARAM_PAGE) != null) {
            pageNumber = Integer.parseInt(request.getParameter(PARAM_PAGE));
            session.setAttribute(PARAM_PAGE, pageNumber);
        } else {
            pageNumber = (Integer)session.getAttribute(PARAM_PAGE);
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }

        Integer pageCount = ((friendsCount-1) / ITEMS_PER_PAGE) + 1;
        Integer indexStart = (pageNumber - 1) * ITEMS_PER_PAGE;
        List<NamedFacebookType> friendsToDisplay = facebookClient.fetchConnection("me/friends", NamedFacebookType.class, Parameter.with("offset", indexStart), Parameter.with("limit", ITEMS_PER_PAGE)).getData();

        out.println("<table border><tr><td width=\"50%\" style=\"vertical-align: top\">");
        out.println("Count of friends: " + friendsCount + "<br>");
        out.println("Page: " + pageNumber + "<br>");
        out.println("Select page: ");
        for (int i=1 ; i<=pageCount ; i++) {
            // TODO: ajax...
            PortletURL url = response.createRenderURL();
            url.setParameter(PARAM_PAGE,  String.valueOf(i));
            out.print("<a href=\"" + url + "\">" + i + "</a> ");
        }
        out.println("<br><br><hr><br>");


        // Collect IDS of friends to display
        List<String> ids = new ArrayList<String>();
        for (NamedFacebookType current : friendsToDisplay) {
            ids.add(current.getId());
        }
        // Fetch them all
        JsonObject friendsResult = facebookClient.fetchObjects(ids, JsonObject.class, Parameter.with("fields", "id,name,picture"));

        for (String id : ids) {
            JsonObject current = friendsResult.getJsonObject(id);
            UserWithPicture friendWithPicture = facebookClient.getJsonMapper().toJavaObject(current.toString(), UserWithPicture.class);

            // TODO: ajax...
            PortletURL urlForPersonDetail = response.createRenderURL();
            urlForPersonDetail.setParameter(PARAM_FRIEND_ID,  friendWithPicture.getId());
            out.println("<img src=\"" + friendWithPicture.getPicture().getData().getUrl() + "\" /><a href=\"" + urlForPersonDetail + "\">" + friendWithPicture.getName() + "</a><br>");
        }
        out.println("</td><td style=\"vertical-align: top\">");

        String friendId = request.getParameter(PARAM_FRIEND_ID);
        if (friendId != null) {
            Connection<StatusMessage> statusMessageConnection = facebookClient.fetchConnection(friendId + "/statuses", StatusMessage.class, Parameter.with("limit", 5));
            List<StatusMessage> statuses = statusMessageConnection.getData();

            if (statuses.size() == 0) {
                out.println("<b>WARNING: </b>This user doesn't have any public messages or you have insufficient scope. Make sure your access token have scopes: <b>email, friends_status</b>");
            } else {
                for (StatusMessage statusMessage : statuses) {
                    out.println("<b>Status message: </b>" + statusMessage.getMessage() + "<br>");
                    out.println("<div style=\"font-size: 13px;\">");
                    out.println("Time: " + statusMessage.getUpdatedTime() + " - ");
                    out.println("<img src=\"TODO:some-thumbs-picture.gif\" alt=\"Likes: " + statusMessage.getLikes().size() + "\" title=\"" + getLikersText(statusMessage.getLikes()) + "\" /></div><br><hr>");

                    List<Comment> comments = statusMessage.getComments();
                    out.println("<b>Comments: </b><br>");
                    for (Comment comment : comments) {
                        out.println("<i>" + comment.getFrom().getName() + "</i>: " + comment.getMessage() + "<br>");
                        out.println("<div style=\"font-size: 11px;\">Time: " + comment.getCreatedTime() + " - Likes: " + comment.getLikeCount() + "</div><br>");
                    }
                    out.println("<br><br><hr>");
                }
            }
        }
        out.println("</td></tr></table>");

        if (log.isTraceEnabled()) {
            log.trace("handleRender finished");
        }
    }

    private String getLikersText(List<NamedFacebookType> likers) {
        StringBuilder builder = new StringBuilder();
        for (NamedFacebookType like : likers) {
            builder.append(like.getName() + "\n");
        }
        return builder.toString();
    }
}
