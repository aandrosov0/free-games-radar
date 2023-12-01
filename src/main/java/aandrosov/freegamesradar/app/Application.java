package aandrosov.freegamesradar.app;

import aandrosov.freegamesradar.GamesRadar;
import aandrosov.freegamesradar.Game;
import aandrosov.freegamesradar.app.social.media.GameUploadException;
import aandrosov.freegamesradar.app.social.media.SocialMediaClient;
import aandrosov.freegamesradar.app.social.media.SocialMediaType;
import aandrosov.freegamesradar.app.social.media.VkGroupSocialMediaClient;
import aandrosov.freegamesradar.epic.games.store.EpicGamesRadar;
import aandrosov.freegamesradar.vk.VkAuthorizator;
import aandrosov.freegamesradar.vk.VkImplicitFlowAuthorizationData;
import com.vk.api.sdk.client.actors.UserActor;

import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public class Application {

    private boolean isDebug;

    public static final String AUTH_DATA_PATH = "auth";

    public static final String SAVED_GAMES_PREFIX_PATH = "_games";

    public static void main(String[] args) {
        new Application().run(args);
    }

    public Application() {
        Thread.currentThread().setUncaughtExceptionHandler(this::onUncaughtException);
    }

    public void run(String[] args) {

        if (args[0].equals("--help")) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("help.txt")) {
                System.out.writeBytes(Objects.requireNonNull(in).readAllBytes());
                return;
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        try {
            isDebug = getArgumentByName("--debug", args) != null;
        } catch (IllegalArgumentException ignored) {
        }

        SocialMediaClient socialMediaClient = parseSocialMediaClientByArgs(args);
        run(socialMediaClient, new EpicGamesRadar("ru"));
    }

    public void run(SocialMediaClient client, GamesRadar... radars) {
        for (GamesRadar radar : radars) {
            List<Game> games = radar.getAllFreeGames(3);

            if (games.isEmpty()) {
                continue;
            }

            for (Game game : games) {
                if (isGamePosted(client.getType(), game)) {
                    System.out.println("Game \"" + game.title() + "\" has already been posted. Skipping...");
                    continue;
                }

                uploadGameAndCatchException(client, game);
                System.out.println("Game \"" + game.title() + "\" has been posted at " + client.getType().name());
            }
        }

        System.out.println("Games are posted!");
    }

    protected void onUncaughtException(Thread thread, Throwable exception) {
        if (isDebug) {
            exception.printStackTrace(System.out);
            return;
        }

        if (exception instanceof NumberFormatException e) {
            System.out.println("Error: " + e.getMessage() + " must be a number");
            return;
        }

        System.out.println("Error: " + exception.getMessage());
    }

    private void uploadGameAndCatchException(SocialMediaClient client, Game game) {
        try {
            client.uploadGame(game);
            saveGameAsPosted(client.getType(), game);
        } catch (GameUploadException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private SocialMediaClient parseSocialMediaClientByArgs(String[] args) {
        SocialMediaType socialMediaType;

        try {
            socialMediaType = SocialMediaType.valueOf(getArgumentByName("--social_media", args).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("You must specify \"--social_media\"");
        }

        if (socialMediaType == SocialMediaType.VK) {
            long appId = Long.parseLong(getArgumentByNameOrThrowRuntimeException("--app_id", args));
            long groupId = Long.parseLong(getArgumentByNameOrThrowRuntimeException("--group_id", args));
            return new VkGroupSocialMediaClient(authorizeVk(appId), groupId);
        }

        return null;
    }

    private UserActor authorizeVk(long appId) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(AUTH_DATA_PATH))) {
            VkImplicitFlowAuthorizationData authData = (VkImplicitFlowAuthorizationData) in.readObject();

            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime < currentTime + authData.getExpiresIn()) {
                return new UserActor(authData.getId(), authData.getAccessToken());
            }
        } catch (FileNotFoundException ignored) {
        } catch (ClassNotFoundException| IOException exception) {
            throw new RuntimeException(exception);
        }

        VkImplicitFlowAuthorizationData authData = new VkAuthorizator().auth(appId, "wall,photos", 5);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(AUTH_DATA_PATH))) {
            out.writeObject(authData);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return new UserActor(authData.getId(), authData.getAccessToken());
    }

    private void saveGameAsPosted(SocialMediaType type, Game game) {
        String filename = type.name().toLowerCase(Locale.ROOT) + SAVED_GAMES_PREFIX_PATH;

        byte[] data = new byte[0];
        try {
            try (InputStream in = new FileInputStream(filename)) {
                data = in.readAllBytes();
            } catch (FileNotFoundException ignored) {
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename), true)) {
                writer.println(new String(data) + game.title() + " " + game.status());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private boolean isGamePosted(SocialMediaType type, Game game) {
        String filename = type.name().toLowerCase(Locale.ROOT) + SAVED_GAMES_PREFIX_PATH;
        try (Scanner scanner = new Scanner(new FileInputStream(filename))) {
            return scanner.findAll(game.title() + " " + game.status()).findAny().isPresent();
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private String getArgumentByName(String name, String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (name.contentEquals(args[i])) {
                    return args[i+1];
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return "";
        }
        throw new IllegalArgumentException("\"" + name + "\" isn't found");
    }

    private String getArgumentByNameOrThrowRuntimeException(String name, String[] args) {
        String value = getArgumentByName(name, args);

        if (value.isEmpty()) {
            throw new RuntimeException("You must specify \"" + name + "\"");
        }

        return value;
    }
}
