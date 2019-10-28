package com.ruinscraft.botboi.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Storage {

    boolean isSetup();

    void insertToken(String token, String discordId);

    void setWaiting(String token, boolean waiting);

    void setUsed(String token, boolean used);

    void setUUID(String token, UUID uuid);

    List<TokenInfo> getWaiting();

    void deleteUser(String discordId);

    boolean isUsed(String token);

    Map<String, UUID> getIDsWithUUIDs();

    String getUsername(UUID uuid);

    String getUsername(String discord_id);

    boolean hasPermission(UUID uuid, String permission);

    boolean groupHasPermission(String group, String permission);

    Collection<String> getPermissionsFromGroup(String group);

    void close();

    default boolean isWaiting(String token) {
        for (TokenInfo tokenInfo : getWaiting()) {
            if (tokenInfo.getToken().equals(token)) {
                return true;
            }
        }
        return false;
    }

    default String generateToken() {
        return UUID.randomUUID().toString().substring(0, 7);
    }

}
