package aandrosov.freegamesradar.epic.games.store;

import aandrosov.freegamesradar.Game;
import aandrosov.freegamesradar.GamesRadar;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EpicGamesRadar implements GamesRadar {

    public static final String URL = "https://store.epicgames.com/";

    private final String locale;

    public EpicGamesRadar(String locale) {
        this.locale = locale;
    }

    @Override
    public List<Game> getAllFreeGames(long timeout) {
        WebDriver driver = new ChromeDriver();
        driver.get(URL + locale);

        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        List<WebElement> webElements = driver.findElement(By.className("css-1vu10h2")).findElements(By.cssSelector("a[aria-label]"));

        List<Game> games = new ArrayList<>();
        for (WebElement webElement : webElements) {
            String[] textData = webElement.getAttribute("aria-label").split(",");
            String title = textData[3].trim();
            String status = textData[2].trim();
            String freePeriod = textData[4].trim();
            String gameUrl = webElement.getAttribute("href");
            String coverUrl = webElement.findElement(By.cssSelector("img[alt=\"" + title + "\"]")).getAttribute("data-image");

            games.add(new EpicGamesGame(gameUrl, title, coverUrl, freePeriod, status));
        }

        driver.quit();
        return games;
    }
}
