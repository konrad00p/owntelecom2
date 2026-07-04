package com.owntelecom.database.model;

public record Zone(
        String id,
        String operatorId,
        ZoneType type,
        String displayName,
        double extraMinute,
        double extraSms,
        double extraMb
) {
}
