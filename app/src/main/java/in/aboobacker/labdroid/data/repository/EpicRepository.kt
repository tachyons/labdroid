package `in`.aboobacker.labdroid.data.repository

import `in`.aboobacker.labdroid.data.model.Discussion
import `in`.aboobacker.labdroid.data.model.Epic
import `in`.aboobacker.labdroid.data.model.Group
import `in`.aboobacker.labdroid.data.model.Issue
import `in`.aboobacker.labdroid.data.remote.GitLabApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpicRepository @Inject constructor(
    private val api: GitLabApi
) {
    suspend fun getGroups(): List<Group> = api.getGroups()

    suspend fun getGroupEpics(groupId: Long): List<Epic> =
        api.getGroupEpics(groupId)

    suspend fun getEpic(groupId: Long, epicIid: Long): Epic =
        api.getEpic(groupId, epicIid)

    suspend fun getEpicIssues(groupId: Long, epicIid: Long): List<Issue> =
        api.getEpicIssues(groupId, epicIid)

    suspend fun getEpicDiscussions(groupId: Long, epicIid: Long): List<Discussion> =
        api.getEpicDiscussions(groupId, epicIid)
}
