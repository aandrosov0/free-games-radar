package aandrosov.freegamesradar.bot;

import aandrosov.freegamesradar.epicgamesstore.EpicGamesGame;
import aandrosov.freegamesradar.epicgamesstore.EpicGamesRadar;
import aandrosov.freegamesradar.Game;
import aandrosov.freegamesradar.vk.VkAuthorizator;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAuthAccessTokenHasExpiredException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiExtendedException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.responses.GetWallUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.SaveWallPhotoResponse;
import com.vk.api.sdk.objects.photos.responses.WallUploadResponse;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Application implements Runnable {

    public static final String CONFIG_PATH = "application.properties";
    public static final String AUTH_PATH = "auth";
    public static final String GAMES_PATH = "games";

    public static final String VK_APP_ID_PROPERTY = "vk.app_id";
    public static final String VK_GROUP_ID_PROPERTY = "vk.group_id";

    private final EpicGamesRadar epicGamesRadar = new EpicGamesRadar("ru");

    private long vkGroupId;
    private long vkAppId;

    public Application() {
        init();
    }

    @Override
    public void run() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, exception) -> {exception.printStackTrace(); System.exit(-1);});

        System.out.println("Application is running!");
        List<Game> games = epicGamesRadar.getAllFreeGames();
        games = games.stream().filter(game -> !isGameHasAlreadyPosted(game)).toList();

        if (games.isEmpty()) {
            return;
        }

        UserActor vkUserActor = authorizeFromConfig();
        System.out.println("Authorization from config...");

        if (vkUserActor == null) {
            System.out.println("Failed!\nAuthorization from browser...");
            vkUserActor = authorizeVk();
        }

        System.out.println("Success!");

        for (Game game : games) {
            postGameAtVkWallFromGroup(vkUserActor, game);
            saveGameAsPosted(game);
        }
    }

    private void init() {
        System.out.println("Loading \"" + CONFIG_PATH + "\" file.");
        Properties properties = new Properties();

        try {
            Files.createFile(Path.of(CONFIG_PATH));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException exception) {
            ApplicationUtils.terminateWithMessage("Cannot to load Config File\n" + exception.getMessage());
        }

        try (InputStream in = new FileInputStream(CONFIG_PATH)) {
            properties.load(in);
        } catch (IOException exception) {
            ApplicationUtils.terminateWithMessage("Cannot to load Config File\n" + exception.getMessage());
        }

        if (ApplicationUtils.isPropertiesEmpty(properties, VK_APP_ID_PROPERTY, VK_GROUP_ID_PROPERTY)) {
            try (Writer writer = new FileWriter(CONFIG_PATH)) {
                properties.store(writer, "Application's Config File");
            } catch (IOException exception) {
                ApplicationUtils.terminateWithMessage("Cannot to store data in Config File\n" + exception.getMessage());
            }
            ApplicationUtils.terminateWithMessage("Error: You must set values!");
        }

        vkGroupId = ApplicationUtils.getLongValueFromPropertiesOrTerminateIfError(properties, VK_GROUP_ID_PROPERTY);
        vkAppId = ApplicationUtils.getLongValueFromPropertiesOrTerminateIfError(properties, VK_APP_ID_PROPERTY);
    }

    private void postGameAtVkWallFromGroup(UserActor userActor, Game game) {
        String message = "";
        if (game instanceof EpicGamesGame epicGamesGame) {
            message += "\uD83D\uDD25Epic Games\n" + epicGamesGame.title() + "\n" + epicGamesGame.status() + "\n" + epicGamesGame.freePeriod() + "\n" + epicGamesGame.url();
        } else {
            message += "\uD83D\uDD25" + game.title() + game.url();
        }

        File gameCoverFile;
        try (InputStream in = new URL(game.coverUrl()).openStream()) {
            gameCoverFile = saveTmpFile(in, ".jpg");
        } catch (IOException exception) {
            ApplicationUtils.terminateWithMessage("Cannot save file: " + exception.getMessage());
            return;
        }

        String photoAttachment;
        try {
            SaveWallPhotoResponse photoResponse = uploadPhotoAtVkWall(userActor, vkGroupId, gameCoverFile);
            photoAttachment = "photo" + photoResponse.getOwnerId() + "_" + photoResponse.getId();
        } catch (ApiAuthAccessTokenHasExpiredException | ApiExtendedException exception) {
            System.out.println("Your token has expired! Reauthorization.");
            postGameAtVkWallFromGroup(authorizeVk(), game);
            return;
        } catch (ClientException | ApiException exception) {
            ApplicationUtils.terminateWithMessage("Error: Cannot upload a game photo \"" + gameCoverFile.getAbsolutePath() + "\"");
            return;
        }

        postAtVkWallFromGroup(userActor, -vkGroupId, message, photoAttachment);
    }

    private void postAtVkWallFromGroup(UserActor userActor, long ownerId, String message, String... attachments) {
        try {
            new VkApiClient(new HttpTransportClient()).wall().post(userActor)
                    .message(message)
                    .ownerId(ownerId)
                    .fromGroup(true)
                    .attachments(attachments)
                    .execute();
        } catch (ApiAuthAccessTokenHasExpiredException | ApiExtendedException exception) {
            System.out.println("Your token has expired! Reauthorization.");
            postAtVkWallFromGroup(authorizeVk(), ownerId, message, attachments);
        } catch (ApiException | ClientException exception) {
            ApplicationUtils.terminateWithMessage("Error: cannot to post at wall\n" + exception.getMessage());
        }
    }

    private SaveWallPhotoResponse uploadPhotoAtVkWall(UserActor actor, long ownerId, File photo) throws ClientException, ApiException {
        VkApiClient api = new VkApiClient(new HttpTransportClient());
        GetWallUploadServerResponse wallUploadServerResponse = api.photos().getWallUploadServer(actor).groupId(ownerId).execute();
        WallUploadResponse wallUploadResponse = api.upload().photoWall(wallUploadServerResponse.getUploadUrl().toASCIIString(), photo).execute();
        return api.photos().saveWallPhoto(actor, wallUploadResponse.getPhoto())
                .server(wallUploadResponse.getServer())
                .hash(wallUploadResponse.getHash())
                .groupId(ownerId)
                .execute()
                .get(0);
    }

    private UserActor authorizeVk() {
        UserActor userActor = new VkAuthorizator().auth(vkAppId, "photos,wall");

        try (PrintWriter writer = new PrintWriter(new FileWriter(AUTH_PATH), true)) {
            writer.print(userActor.getId() + "\n");
            writer.println(userActor.getAccessToken());
        } catch (IOException exception) {
            ApplicationUtils.terminateWithMessage("Error: cannot write data at file \"" + AUTH_PATH + "\"\n" + exception.getMessage());
        }

        return userActor;
    }

    private UserActor authorizeFromConfig() {
        long userId;
        String token;
        try (Scanner scanner = new Scanner(new FileInputStream(AUTH_PATH))) {
            userId = Long.parseLong(scanner.nextLine());
            token = scanner.nextLine();
        } catch (Exception exception) {
            return null;
        }

        return new UserActor(userId, token);
    }

    private File saveTmpFile(InputStream in, String suffix) throws IOException {
        Path path = Files.createTempFile(null, suffix);
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        return path.toFile();
    }

    private boolean isGameHasAlreadyPosted(Game game) {
        try (Scanner scanner = new Scanner(new FileInputStream(GAMES_PATH))) {
            while (scanner.hasNextLine()) {
                String[] gameData = scanner.nextLine().split(",");
                String title = gameData[0];
                String status = gameData[1];

                if (title.contentEquals(game.title()) && status.contentEquals(game.status())) {
                    return true;
                }
            }
        } catch (FileNotFoundException ignored) {
            return false;
        }
        return false;
    }

    private void saveGameAsPosted(Game game) {
        try {
            Files.createFile(Path.of(GAMES_PATH));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        byte[] oldData = new byte[0];
        try (InputStream in = new FileInputStream(GAMES_PATH)) {
            oldData = in.readAllBytes();
        } catch (IOException ignored) {
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(GAMES_PATH), true)) {
            writer.print(new String(oldData));
            writer.println(game.title() + "," + game.status());
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
