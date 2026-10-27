package com.skillswap.modules.session.entity;

/**
 * Which kind of session a note belongs to. Both swap sessions and shared learning
 * learning sessions share the same notes API, but their records live in
 * different tables, so the note keeps the kind alongside the id.
 */
public enum SessionType {
    SWAP,
    LEARNING
}
