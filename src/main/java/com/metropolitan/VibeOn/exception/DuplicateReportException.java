package com.metropolitan.VibeOn.exception;

public class DuplicateReportException extends RuntimeException {
    public DuplicateReportException() {
        super("An active report already exists for this content.");
    }
}
