package com.metropolitan.VibeOn.exception;

public class ReportTargetNotFoundException extends RuntimeException {
    public ReportTargetNotFoundException() {
        super("The reported content was not found.");
    }
}
