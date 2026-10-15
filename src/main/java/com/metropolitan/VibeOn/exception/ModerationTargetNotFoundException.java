package com.metropolitan.VibeOn.exception;

public class ModerationTargetNotFoundException extends RuntimeException {
    public ModerationTargetNotFoundException() {
        super("The content selected for moderation was not found.");
    }
}
