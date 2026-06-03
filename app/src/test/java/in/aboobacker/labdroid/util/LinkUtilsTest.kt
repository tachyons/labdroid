package `in`.aboobacker.labdroid.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkUtilsTest {

    @Test
    fun testParseIssueReference() {
        val link = LinkUtils.parseReference("#123", currentProjectId = 1L)
        assertTrue(link is GitLabLink.Issue)
        assertEquals(1L, (link as GitLabLink.Issue).projectId)
        assertEquals(123L, link.iid)
    }

    @Test
    fun testParseCrossProjectIssueReference() {
        val link = LinkUtils.parseReference("group/project#123")
        assertTrue(link is GitLabLink.Issue)
        assertEquals("group/project", (link as GitLabLink.Issue).projectPath)
        assertEquals(123L, link.iid)
    }

    @Test
    fun testParseMrReference() {
        val link = LinkUtils.parseReference("!456", currentProjectId = 1L)
        assertTrue(link is GitLabLink.MergeRequest)
        assertEquals(1L, (link as GitLabLink.MergeRequest).projectId)
        assertEquals(456L, link.iid)
    }

    @Test
    fun testParseUserReference() {
        val link = LinkUtils.parseReference("@username")
        assertTrue(link is GitLabLink.User)
        assertEquals("username", (link as GitLabLink.User).username)
    }
}
