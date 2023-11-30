package aandrosov.freegamesradar.epicgamesstore;

import aandrosov.freegamesradar.Game;

public record EpicGamesGame(String url, String title, String coverUrl, String freePeriod, String status) implements Game {
}
