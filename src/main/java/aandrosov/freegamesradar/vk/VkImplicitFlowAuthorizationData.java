package aandrosov.freegamesradar.vk;

import com.vk.api.sdk.client.actors.Actor;

import java.io.*;

public class VkImplicitFlowAuthorizationData implements Actor<Long>, Serializable {

    private final String accessToken;

    private final Long expiresIn;
    private final Long userId;

    public VkImplicitFlowAuthorizationData(String accessToken, Long expiresIn, Long userId) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
    }

    protected VkImplicitFlowAuthorizationData() {
        this(null, null, null);
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public Long getId() {
        return userId;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    @Override
    public String toString() {
        return "VkImplicitFlowUserActor{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", userId=" + userId +
                '}';
    }
}
