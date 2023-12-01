package aandrosov.freegamesradar.app.social.media;

import aandrosov.freegamesradar.Game;
import aandrosov.freegamesradar.app.ApplicationUtils;
import aandrosov.freegamesradar.epic.games.store.EpicGamesGame;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.responses.GetWallUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.SaveWallPhotoResponse;
import com.vk.api.sdk.objects.photos.responses.WallUploadResponse;

import java.io.File;
import java.io.IOException;

public class VkGroupSocialMediaClient implements SocialMediaClient {

    private final UserActor actor;
    private final long groupId;

    private final VkApiClient api;

    public VkGroupSocialMediaClient(UserActor actor, long groupId) {
        this.actor = actor;
        this.groupId = groupId;
        this.api = new VkApiClient(new HttpTransportClient());
    }

    @Override
    public void uploadGame(Game game) throws GameUploadException {
        try {
            String message = game.title() + "\n" + game.status() + "\n" + game.url();

            if (game instanceof EpicGamesGame) {
                message = "\uD83D\uDD25Epic Games\n" + ((EpicGamesGame) game).freePeriod() + "\n" + message;
            }

            File gameCoverFile = ApplicationUtils.saveTempFileFromUrl(game.coverUrl(), null, ".jpg").toFile();
            SaveWallPhotoResponse photo = uploadWallPhoto(gameCoverFile);
            String attachment = "photo" + photo.getOwnerId() + "_" + photo.getId();
            api.wall().post(actor).fromGroup(true).ownerId(-groupId).message(message).attachments(attachment).execute();
        } catch (ClientException | IOException | ApiException exception) {
            throw new GameUploadException("Cannot to upload a game \"" + game.title() + "\": " + exception.getMessage());
        }
    }

    public SaveWallPhotoResponse uploadWallPhoto(File photo) throws ClientException, ApiException {
        GetWallUploadServerResponse uploadServer = api.photos().getWallUploadServer(actor).groupId(groupId).execute();

        String uploadUrl = uploadServer.getUploadUrl().toASCIIString();
        WallUploadResponse wallUpload = api.upload().photoWall(uploadUrl, photo).execute();

        return api.photos().saveWallPhoto(actor, wallUpload.getPhoto())
                .server(wallUpload.getServer())
                .hash(wallUpload.getHash())
                .groupId(groupId)
                .execute()
                .get(0);
    }

    @Override
    public SocialMediaType getType() {
        return SocialMediaType.VK;
    }
}
