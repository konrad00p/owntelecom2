package com.owntelecom.database.model;

import java.util.UUID;

public record Datacenter(
        int id,
        UUID ownerUuid,
        String world,
        int x,
        int y,
        int z,
        int level,
        boolean broken
) {
}
