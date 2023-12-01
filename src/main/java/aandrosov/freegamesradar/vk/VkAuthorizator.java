package aandrosov.freegamesradar.vk;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VkAuthorizator {

    public static final String URL = "https://oauth.vk.com/authorize?";
    public static final String AUTH_BLANK_URL = "https://oauth.vk.com/blank.html#";

    public VkImplicitFlowAuthorizationData auth(long appId, String scope, long timeout) {
        WebDriver driver = new ChromeDriver();
        driver.get(URL + "client_id=" + appId + "&display=page&scope=" + scope + "&response_type=token");

        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (!driver.getCurrentUrl().startsWith(AUTH_BLANK_URL));
        String url = driver.getCurrentUrl();
        driver.quit();

        return parseImplicitFlowAuthorizationUrl(url);
    }

    public VkImplicitFlowAuthorizationData parseImplicitFlowAuthorizationUrl(String url) {
        Matcher matcher = Pattern.compile("(\\w+)=([\\w.\\-_]+)").matcher(url);

        long userId = 0, expiresIn = 0;
        String accessToken = "", error = null, errorDescription = null;

        while (matcher.find()) {
            switch (matcher.group(1)) {
                case "access_token":
                    accessToken = matcher.group(2);
                    break;
                case "expires_in":
                    expiresIn = Long.parseLong(matcher.group(2));
                    break;
                case "user_id":
                    userId = Long.parseLong(matcher.group(2));
                    break;
                case "error":
                    error = matcher.group(2);
                    break;
                case "error_description":
                    errorDescription = matcher.group(2);
                    break;
            }
        }

        if (error != null || errorDescription != null) {
            throw new IllegalArgumentException(error + " " + errorDescription);
        }

        return new VkImplicitFlowAuthorizationData(accessToken, expiresIn, userId);
    }
}
