package org.gatein.security.oauth.portlet.google;

import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.CommentFeed;

/**
 * Created with IntelliJ IDEA.
 * User: vrockai
 * Date: 4/14/13
 * Time: 10:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class GoogleActivityBean {

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    Activity activity;

    public CommentFeed getCommentFeed() {
        return commentFeed;
    }

    public void setCommentFeed(CommentFeed commentFeed) {
        this.commentFeed = commentFeed;
    }

    CommentFeed commentFeed;

    public GoogleActivityBean(Activity activity){
        this.activity = activity;
    }


}
