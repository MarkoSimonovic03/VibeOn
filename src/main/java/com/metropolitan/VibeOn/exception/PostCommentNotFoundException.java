package com.metropolitan.VibeOn.exception;

public class PostCommentNotFoundException extends RuntimeException {
    public PostCommentNotFoundException() {
        super("Post or comment not found.");
    }
}
