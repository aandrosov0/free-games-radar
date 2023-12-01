package aandrosov.freegamesradar;

import java.io.Serializable;

public interface Game extends Serializable {

    String title();

    String coverUrl();

    String url();

    String status();
}

