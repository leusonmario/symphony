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
package org.b3log.symphony.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.Pagination;
import org.b3log.latke.repository.CompositeFilter;
import org.b3log.latke.repository.CompositeFilterOperator;
import org.b3log.latke.repository.Filter;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.symphony.model.Article;
import org.b3log.symphony.model.Follow;
import org.b3log.symphony.repository.ArticleRepository;
import org.b3log.symphony.repository.FollowRepository;
import org.b3log.symphony.repository.TagRepository;
import org.b3log.symphony.repository.UserRepository;
import org.b3log.symphony.util.Filler;
import org.json.JSONObject;

/**
 * Follow query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.2.0.1, Jun 3, 2015
 * @since 0.2.5
 */
@Service
public class FollowQueryService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(FollowQueryService.class.getName());

    /**
     * Follow repository.
     */
    @Inject
    private FollowRepository followRepository;

    /**
     * User repository.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Tag repository.
     */
    @Inject
    private TagRepository tagRepository;

    /**
     * Article repository.
     */
    @Inject
    private ArticleRepository articleRepository;

    /**
     * Filler.
     */
    @Inject
    private Filler filler;

    /**
     * Determines whether exists a follow relationship for the specified follower and the specified following entity.
     *
     * @param followerId the specified follower id
     * @param followingId the specified following entity id
     * @return {@code true} if exists, returns {@code false} otherwise
     */
    public boolean isFollowing(final String followerId, final String followingId) {
        try {
            return followRepository.exists(followerId, followingId);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Determines following failed[followerId=" + followerId + ", followingId="
                    + followingId + ']', e);

            return false;
        }
    }

    /**
     * Gets following users of the specified follower.
     *
     * @param followerId the specified follower id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         User
     *     }, ....]
     * }
     * </pre>
     *
     * @throws ServiceException service exception
     */
    public JSONObject getFollowingUsers(final String followerId, final int currentPageNum, final int pageSize) throws ServiceException {
        final JSONObject ret = new JSONObject();
        final List<JSONObject> records = new ArrayList<JSONObject>();

        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, 0);

        try {
            final JSONObject result = getFollowings(followerId, Follow.FOLLOWING_TYPE_C_USER, currentPageNum, pageSize);
            @SuppressWarnings("unchecked")
            final List<JSONObject> followings = (List<JSONObject>) result.opt(Keys.RESULTS);

            for (final JSONObject follow : followings) {
                final String followingId = follow.optString(Follow.FOLLOWING_ID);
                final JSONObject user = userRepository.get(followingId);

                if (null == user) {
                    LOGGER.log(Level.WARN, "Not found user[id=" + followingId + ']');

                    continue;
                }

                filler.fillUserThumbnailURL(user);

                records.add(user);
            }

            ret.put(Pagination.PAGINATION_RECORD_COUNT, result.optInt(Pagination.PAGINATION_RECORD_COUNT));
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets following users of follower[id=" + followerId + "] failed", e);
        }

        return ret;
    }

    /**
     * Gets following tags of the specified follower.
     *
     * @param followerId the specified follower id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         Tag
     *     }, ....]
     * }
     * </pre>
     *
     * @throws ServiceException service exception
     */
    public JSONObject getFollowingTags(final String followerId, final int currentPageNum, final int pageSize) throws ServiceException {
        final JSONObject ret = new JSONObject();
        final List<JSONObject> records = new ArrayList<JSONObject>();

        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, 0);

        try {
            final JSONObject result = getFollowings(followerId, Follow.FOLLOWING_TYPE_C_TAG, currentPageNum, pageSize);
            @SuppressWarnings("unchecked")
            final List<JSONObject> followings = (List<JSONObject>) result.opt(Keys.RESULTS);

            for (final JSONObject follow : followings) {
                final String followingId = follow.optString(Follow.FOLLOWING_ID);
                final JSONObject tag = tagRepository.get(followingId);

                if (null == tag) {
                    LOGGER.log(Level.WARN, "Not found tag[id=" + followingId + ']');

                    continue;
                }

                records.add(tag);
            }

            ret.put(Pagination.PAGINATION_RECORD_COUNT, result.optInt(Pagination.PAGINATION_RECORD_COUNT));
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets following tags of follower[id=" + followerId + "] failed", e);
        }

        return ret;
    }

    /**
     * Gets following articles of the specified follower.
     *
     * @param followerId the specified follower id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         Article
     *     }, ....]
     * }
     * </pre>
     *
     * @throws ServiceException service exception
     */
    public JSONObject getFollowingArticles(final String followerId, final int currentPageNum, final int pageSize) throws ServiceException {
        final JSONObject ret = new JSONObject();
        final List<JSONObject> records = new ArrayList<JSONObject>();

        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, 0);

        try {
            final JSONObject result = getFollowings(followerId, Follow.FOLLOWING_TYPE_C_ARTICLE, currentPageNum, pageSize);
            @SuppressWarnings("unchecked")
            final List<JSONObject> followings = (List<JSONObject>) result.opt(Keys.RESULTS);

            for (final JSONObject follow : followings) {
                final String followingId = follow.optString(Follow.FOLLOWING_ID);
                final JSONObject article = articleRepository.get(followingId);

                if (null == article) {
                    LOGGER.log(Level.WARN, "Not found article[id=" + followingId + ']');

                    continue;
                }

                article.put(Article.ARTICLE_CREATE_TIME, new Date(article.optLong(Article.ARTICLE_CREATE_TIME)));
                article.put(Article.ARTICLE_UPDATE_TIME, new Date(article.optLong(Article.ARTICLE_UPDATE_TIME)));
                article.put(Article.ARTICLE_LATEST_CMT_TIME, new Date(article.optLong(Article.ARTICLE_LATEST_CMT_TIME)));

                records.add(article);
            }

            ret.put(Pagination.PAGINATION_RECORD_COUNT, result.optInt(Pagination.PAGINATION_RECORD_COUNT));
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets following articles of follower[id=" + followerId + "] failed", e);
        }

        return ret;
    }

    /**
     * Gets follower users of the specified following user.
     *
     * @param followingUserId the specified following user id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         User
     *     }, ....]
     * }
     * </pre>
     *
     * @throws ServiceException service exception
     */
    public JSONObject getFollowerUsers(final String followingUserId, final int currentPageNum, final int pageSize)
            throws ServiceException {
        final JSONObject ret = new JSONObject();
        final List<JSONObject> records = new ArrayList<JSONObject>();

        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, 0);

        try {
            final JSONObject result = getFollowers(followingUserId, Follow.FOLLOWING_TYPE_C_USER, currentPageNum, pageSize);

            @SuppressWarnings("unchecked")
            final List<JSONObject> followers = (List<JSONObject>) result.opt(Keys.RESULTS);

            for (final JSONObject follow : followers) {
                final String followerId = follow.optString(Follow.FOLLOWER_ID);
                final JSONObject user = userRepository.get(followerId);

                if (null == user) {
                    LOGGER.log(Level.WARN, "Not found user[id=" + followerId + ']');

                    continue;
                }

                filler.fillUserThumbnailURL(user);

                records.add(user);
            }

            ret.put(Pagination.PAGINATION_RECORD_COUNT, result.optInt(Pagination.PAGINATION_RECORD_COUNT));
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets follower users of following user[id=" + followingUserId + "] failed", e);
        }

        return ret;
    }

    /**
     * Gets the followings of a follower specified by the given follower id and following type.
     *
     * @param followerId the given follower id
     * @param followingType the specified following type
     * @param currentPageNum the specified current page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         "oId": "",
     *         "followerId": "",
     *         "followingId": "",
     *         "followingType": int
     *     }, ....]
     * }
     * </pre>
     *
     * @throws RepositoryException repository exception
     */
    private JSONObject getFollowings(final String followerId, final int followingType, final int currentPageNum, final int pageSize)
            throws RepositoryException {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Follow.FOLLOWER_ID, FilterOperator.EQUAL, followerId));
        filters.add(new PropertyFilter(Follow.FOLLOWING_TYPE, FilterOperator.EQUAL, followingType));

        final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
                .setPageSize(pageSize).setCurrentPageNum(currentPageNum);

        final JSONObject result = followRepository.get(query);
        final List<JSONObject> records = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
        final int recordCnt = result.optJSONObject(Pagination.PAGINATION).optInt(Pagination.PAGINATION_RECORD_COUNT);

        final JSONObject ret = new JSONObject();
        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, recordCnt);

        return ret;
    }

    /**
     * Gets the followers of a following specified by the given following id and follow type.
     *
     * @param followingId the given following id
     * @param followingType the specified following type
     * @param currentPageNum the specified current page number
     * @param pageSize the specified page size
     * @return result json object, for example,      <pre>
     * {
     *     "paginationRecordCount": int,
     *     "rslts": java.util.List[{
     *         "oId": "",
     *         "followerId": "",
     *         "followingId": "",
     *         "followingType": int
     *     }, ....]
     * }
     * </pre>
     *
     * @throws RepositoryException repository exception
     */
    private JSONObject getFollowers(final String followingId, final int followingType, final int currentPageNum, final int pageSize)
            throws RepositoryException {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Follow.FOLLOWING_ID, FilterOperator.EQUAL, followingId));
        filters.add(new PropertyFilter(Follow.FOLLOWING_TYPE, FilterOperator.EQUAL, followingType));

        final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
                .setPageSize(pageSize).setCurrentPageNum(currentPageNum);

        final JSONObject result = followRepository.get(query);

        final List<JSONObject> records = CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
        final int recordCnt = result.optJSONObject(Pagination.PAGINATION).optInt(Pagination.PAGINATION_RECORD_COUNT);

        final JSONObject ret = new JSONObject();
        ret.put(Keys.RESULTS, (Object) records);
        ret.put(Pagination.PAGINATION_RECORD_COUNT, recordCnt);

        return ret;
    }
}
