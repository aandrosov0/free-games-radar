package aandrosov.freegamesradar.app.social.media;

import aandrosov.freegamesradar.Game;

public interface SocialMediaClient {

    void uploadGame(Game game) throws GameUploadException;

    SocialMediaType getType();
}
