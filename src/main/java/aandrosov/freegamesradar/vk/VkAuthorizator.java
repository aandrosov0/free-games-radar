package aandrosov.freegamesradar.vk;

import com.vk.api.sdk.client.actors.UserActor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VkAuthorizator {

    public static final String URL = "https://oauth.vk.com/authorize?";
    public static final String AUTH_BLANK_URL = "https://oauth.vk.com/blank.html#";

    public UserActor auth(long appId, String scope) {
        WebDriver driver = new ChromeDriver();
        driver.get(URL + "client_id=" + appId + "&display=page&scope=" + scope + "&response_type=token");

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (!driver.getCurrentUrl().startsWith(AUTH_BLANK_URL));
        String url = driver.getCurrentUrl();
        driver.quit();

        return parseImplicitFlowAuthorizationUrl(url);
    }

    public UserActor parseImplicitFlowAuthorizationUrl(String url) {
        Matcher matcher = Pattern.compile("(\\w+)=([\\w.\\-_]+)").matcher(url);

        Long userId = null;
        String accessToken = "";
        while (matcher.find()) {
            switch (matcher.group(1)) {
                case "access_token":
                    accessToken = matcher.group(2);
                    break;
                case "user_id":
                    userId = Long.parseLong(matcher.group(2));
                    break;
            }
        }

        return new UserActor(userId, accessToken);
    }
}
