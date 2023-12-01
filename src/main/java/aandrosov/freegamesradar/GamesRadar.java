package aandrosov.freegamesradar;

import java.util.List;

public interface GamesRadar {

    List<Game> getAllFreeGames(long timeout);
}
