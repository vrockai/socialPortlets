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

package org.gatein.security.oauth.portlet.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
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
import org.gatein.security.oauth.google.GoogleAccessTokenContext;
import org.gatein.security.oauth.google.GoogleProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleActivitiesPortlet extends AbstractSocialPortlet<GoogleAccessTokenContext> {

    public static final String REQUIRED_SCOPE = "https://www.googleapis.com/auth/plus.login";
    private GoogleProcessor googleProcessor;

    @Override
    protected void afterInit(ExoContainer container) {
        this.googleProcessor = (GoogleProcessor)container.getComponentInstanceOfType(GoogleProcessor.class);
    }


    @Override
    protected OAuthProviderType<GoogleAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_GOOGLE);
    }

    // See https://developers.google.com/+/api/latest/activities/list for details
    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, GoogleAccessTokenContext accessToken) throws PortletException, IOException {
        final Plus service = googleProcessor.getPlusService(accessToken);
        final Plus.Activities.List list  = service.activities().list("me", "public");
        list.setMaxResults(10L);

        ActivityFeed activityFeed = new GoogleRequest<ActivityFeed>(request, response, getPortletContext(), getOAuthProvider(), REQUIRED_SCOPE) {

            @Override
            protected ActivityFeed execute() throws IOException {
                return list.execute();
            }

        }.sendRequest();

        List<GoogleActivityBean> googleActivityBeanList = new ArrayList<GoogleActivityBean>();

        if (activityFeed != null) {
            for (final Activity activity : activityFeed.getItems()) {

                GoogleActivityBean gab = new GoogleActivityBean(activity);
                CommentFeed comments = new GoogleRequest<CommentFeed>(request, response, getPortletContext(), getOAuthProvider(), REQUIRED_SCOPE) {

                    @Override
                    protected CommentFeed execute() throws IOException {
                        return service.comments().list(activity.getId()).execute();
                    }

                }.sendRequest();

                gab.setCommentFeed(comments);

                googleActivityBeanList.add(gab);
            }
        }

        request.setAttribute("googleActivityBeanList", googleActivityBeanList);
        PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/jsp/google/activities.jsp");
        prd.include(request, response);
    }
}
