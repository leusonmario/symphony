/*
 * Copyright (c) 2012-2015, b3log.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.util;

import org.b3log.symphony.service.ThumbnailQueryService;
import java.util.Calendar;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.Role;
import org.b3log.latke.model.User;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.user.UserService;
import org.b3log.latke.user.UserServiceFactory;
import org.b3log.latke.util.Sessions;
import org.b3log.symphony.SymphonyServletListener;
import org.b3log.symphony.model.Common;
import org.b3log.symphony.model.Notification;
import org.b3log.symphony.model.Option;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.service.ArticleQueryService;
import org.b3log.symphony.service.CommentQueryService;
import org.b3log.symphony.service.NotificationQueryService;
import org.b3log.symphony.service.OptionQueryService;
import org.b3log.symphony.service.TagQueryService;
import org.b3log.symphony.service.UserMgmtService;
import org.b3log.symphony.service.UserQueryService;
import org.json.JSONObject;

/**
 * Filler utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.10, May 12, 2015
 * @since 0.2.0
 */
@Service
public class Filler {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Filler.class.getName());

    /**
     * Language service.
     */
    @Inject
    private LangPropsService langPropsService;

    /**
     * User service.
     */
    private UserService userService = UserServiceFactory.getUserService();

    /**
     * Article query service.
     */
    @Inject
    private ArticleQueryService articleQueryService;

    /**
     * Comment query service.
     */
    @Inject
    private CommentQueryService commentQueryService;

    /**
     * Tag query service.
     */
    @Inject
    private TagQueryService tagQueryService;

    /**
     * Option query service.
     */
    @Inject
    private OptionQueryService optionQueryService;

    /**
     * User management service.
     */
    @Inject
    private UserMgmtService userMgmtService;

    /**
     * User query service.
     */
    @Inject
    private UserQueryService userQueryService;

    /**
     * Notification query service.
     */
    @Inject
    private NotificationQueryService notificationQueryService;

    /**
     * Thumbnail query service.
     */
    @Inject
    private ThumbnailQueryService thumbnailQueryService;

    /**
     * Fills relevant articles.
     *
     * @param dataModel the specified data model
     * @param article the specified article
     * @throws Exception exception
     */
    public void fillRelevantArticles(final Map<String, Object> dataModel, final JSONObject article) throws Exception {
        dataModel.put(Common.SIDE_RELEVANT_ARTICLES,
                      articleQueryService.getRelevantArticles(article, Symphonys.getInt("sideRelevantArticlesCnt")));
    }

    /**
     * Fills the latest comments.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillLatestCmts(final Map<String, Object> dataModel) throws Exception {
        dataModel.put(Common.SIDE_LATEST_CMTS, commentQueryService.getLatestComments(Symphonys.getInt("sizeLatestCmtsCnt")));
    }

    /**
     * Fills random articles.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillRandomArticles(final Map<String, Object> dataModel) throws Exception {
        dataModel.put(Common.SIDE_RANDOM_ARTICLES, articleQueryService.getRandomArticles(Symphonys.getInt("sideRandomArticlesCnt")));
    }

    /**
     * Fills tags.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillSideTags(final Map<String, Object> dataModel) throws Exception {
        dataModel.put(Common.SIDE_TAGS, tagQueryService.getTags(Symphonys.getInt("sideTagsCnt")));
    }

    /**
     * Fills header.
     *
     * @param request the specified request
     * @param response the specified response
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillHeader(final HttpServletRequest request, final HttpServletResponse response,
                           final Map<String, Object> dataModel) throws Exception {
        fillMinified(dataModel);
        dataModel.put(Common.STATIC_RESOURCE_VERSION, Latkes.getStaticResourceVersion());

        fillTrendTags(dataModel);
        fillPersonalNav(request, response, dataModel);

        fillLangs(dataModel);
    }

    /**
     * Fills footer.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillFooter(final Map<String, Object> dataModel) throws Exception {
        fillSysInfo(dataModel);

        dataModel.put(Common.YEAR, String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        dataModel.put(Common.SITE_VISIT_STAT_CODE, Symphonys.get("siteVisitStatCode"));
    }

    /**
     * Fills header and footer.
     *
     * @param request the specified request
     * @param response the specified response
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    public void fillHeaderAndFooter(final HttpServletRequest request, final HttpServletResponse response,
                                    final Map<String, Object> dataModel) throws Exception {
        fillHeader(request, response, dataModel);
        fillFooter(dataModel);
    }

    /**
     * Fills personal navigation.
     *
     * @param request the specified request
     * @param response the specified response
     * @param dataModel the specified data model
     */
    private void fillPersonalNav(final HttpServletRequest request, final HttpServletResponse response,
                                 final Map<String, Object> dataModel) {
        dataModel.put(Common.IS_LOGGED_IN, false);
        dataModel.put(Common.IS_ADMIN_LOGGED_IN, false);

        if (null == Sessions.currentUser(request) && !userMgmtService.tryLogInWithCookie(request, response)) {
            dataModel.put("loginLabel", langPropsService.get("loginLabel"));

            return;
        }

        JSONObject curUser = null;

        try {
            curUser = userQueryService.getCurrentUser(request);
        } catch (final ServiceException e) {
            LOGGER.log(Level.ERROR, "Gets the current user failed", e);
        }

        if (null == curUser) {
            dataModel.put("loginLabel", langPropsService.get("loginLabel"));

            return;
        }

        dataModel.put(Common.IS_LOGGED_IN, true);
        dataModel.put(Common.LOGOUT_URL, userService.createLogoutURL("/"));

        dataModel.put("logoutLabel", langPropsService.get("logoutLabel"));

        final String userName = curUser.optString(User.USER_NAME);
        dataModel.put(User.USER_NAME, userName);
        final String userRole = curUser.optString(User.USER_ROLE);
        dataModel.put(User.USER_ROLE, userRole);
        dataModel.put(Common.IS_ADMIN_LOGGED_IN, Role.ADMIN_ROLE.equals(userRole));

        fillUserThumbnailURL(curUser);

        dataModel.put(Common.CURRENT_USER, curUser);

        final int unreadNotificationCount = notificationQueryService.getUnreadNotificationCount(curUser.optString(Keys.OBJECT_ID));
        dataModel.put(Notification.NOTIFICATION_T_UNREAD_COUNT, unreadNotificationCount);
    }

    /**
     * Fills minified directory and file postfix for static JavaScript, CSS.
     *
     * @param dataModel the specified data model
     */
    public void fillMinified(final Map<String, Object> dataModel) {
        switch (Latkes.getRuntimeMode()) {
            case DEVELOPMENT:
                dataModel.put(Common.MINI_POSTFIX, "");
                break;
            case PRODUCTION:
                dataModel.put(Common.MINI_POSTFIX, Common.MINI_POSTFIX_VALUE);
                break;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Fills the specified user thumbnail URL.
     *
     * @param user the specified user
     */
    public void fillUserThumbnailURL(final JSONObject user) {
        final int avatarType = user.optInt(UserExt.USER_AVATAR_TYPE);

        if (UserExt.USER_AVATAR_TYPE_C_GRAVATAR == avatarType) {
            final String userEmail = user.optString(User.USER_EMAIL);
            final String thumbnailURL = thumbnailQueryService.getGravatarURL(userEmail, "140");
            user.put(UserExt.USER_T_THUMBNAIL_URL, thumbnailURL);
        } else if (UserExt.USER_AVATAR_TYPE_C_EXTERNAL_LINK == avatarType) {
            user.put(UserExt.USER_T_THUMBNAIL_URL, user.optString(UserExt.USER_AVATAR_URL));
        }
    }

    /**
     * Fills the all language labels.
     *
     * @param dataModel the specified data model
     */
    private void fillLangs(final Map<String, Object> dataModel) {
        dataModel.putAll(langPropsService.getAll(Latkes.getLocale()));
    }

    /**
     * Fills trend tags.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    private void fillTrendTags(final Map<String, Object> dataModel) throws Exception {
        dataModel.put(Common.NAV_TREND_TAGS, tagQueryService.getTrendTags(Symphonys.getInt("trendTagsCnt")));
    }

    /**
     * Fills system info.
     *
     * @param dataModel the specified data model
     * @throws Exception exception
     */
    private void fillSysInfo(final Map<String, Object> dataModel) throws Exception {
        dataModel.put(Common.VERSION, SymphonyServletListener.VERSION);
        dataModel.put(Common.ONLINE_VISITOR_CNT, optionQueryService.getOnlineVisitorCount());

        final JSONObject statistic = optionQueryService.getStatistic();
        dataModel.put(Option.CATEGORY_C_STATISTIC, statistic);
    }
}
