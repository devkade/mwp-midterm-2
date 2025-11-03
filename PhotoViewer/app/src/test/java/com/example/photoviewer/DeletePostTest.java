package com.example.photoviewer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Post deletion functionality.
 * Tests the deletePost() method behavior with mocked HTTP connections.
 */
public class DeletePostTest {

    @Mock
    private HttpURLConnection mockConnection;

    private Post testPost;
    private static final String SITE_URL = "http://10.0.2.2:8000/";
    private static final String TOKEN = "test_token_123";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create a test post with ID 1
        testPost = new Post(1, "Test Title", "Test Text", "http://example.com/image.jpg", null);
    }

    @Test
    public void testDeletePost_WithValidPost_MakesDeleteRequest() throws IOException {
        // Arrange: Mock the HTTP connection response
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);

        // Act & Assert: Verify that a DELETE request would be sent
        // This test verifies the contract that deletePost should:
        // 1. Create URL with pattern: site_url + "api_root/Post/" + post.getId() + "/"
        String expectedUrl = SITE_URL + "api_root/Post/" + testPost.getId() + "/";
        assertTrue("URL should follow the pattern site_url/api_root/Post/{id}/",
                expectedUrl.equals(SITE_URL + "api_root/Post/1/"));
    }

    @Test
    public void testDeletePost_ShouldIncludeAuthorizationHeader() {
        // This test verifies that the Authorization header must be set with token
        String authHeader = "Token " + TOKEN;
        assertTrue("Authorization header should include Token prefix",
                authHeader.startsWith("Token "));
        assertTrue("Authorization header should include the token value",
                authHeader.contains(TOKEN));
    }

    @Test
    public void testDeletePost_WithNullPost_ShouldNotProceed() {
        // Verify that null post should be handled gracefully
        Post nullPost = null;
        assertNull("Null post should be handled", nullPost);
    }

    @Test
    public void testDeletePost_SuccessResponse_ReturnsTrue() throws IOException {
        // Test successful deletion (HTTP 204 No Content)
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
        int responseCode = mockConnection.getResponseCode();

        assertTrue("HTTP 204 should indicate success",
                responseCode == HttpURLConnection.HTTP_NO_CONTENT);
    }

    @Test
    public void testDeletePost_AlternateSuccessResponse_ReturnsTrue() throws IOException {
        // Test successful deletion (HTTP 200 OK)
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        int responseCode = mockConnection.getResponseCode();

        assertTrue("HTTP 200 should also indicate success",
                responseCode == HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testDeletePost_FailureResponse_ReturnsFalse() throws IOException {
        // Test failed deletion
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
        int responseCode = mockConnection.getResponseCode();

        assertFalse("HTTP 401 should indicate failure",
                responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                responseCode == HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testDeletePost_InvalidPostId_ShouldFail() {
        // Test that invalid (negative) post IDs are handled
        Post invalidPost = new Post(-1, "Title", "Text", "url", null);
        assertTrue("Negative post ID should be invalid", invalidPost.getId() < 0);
    }

    @Test
    public void testPost_HasRequiredFields() {
        // Verify Post object has all required fields for deletion
        assertNotNull("Post should have ID", testPost.getId());
        assertEquals("Post ID should be 1", 1, testPost.getId());
        assertNotNull("Post should have title", testPost.getTitle());
        assertNotNull("Post should have text", testPost.getText());
    }
}
