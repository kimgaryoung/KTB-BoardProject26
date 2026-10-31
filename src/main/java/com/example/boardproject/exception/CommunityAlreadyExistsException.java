package com.example.boardproject.exception;

import lombok.Getter;

@Getter
public class CommunityAlreadyExistsException extends RuntimeException {
    private final Long communityId;

    public CommunityAlreadyExistsException(Long communityId) {
        super("COMMUNITY_ALREADY_EXISTS");
        this.communityId = communityId;
    }
}
