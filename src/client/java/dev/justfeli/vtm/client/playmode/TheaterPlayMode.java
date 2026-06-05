package dev.justfeli.vtm.client.playmode;

public enum TheaterPlayMode {
    STANDING("vivecraft.options.standing", false),
    SEATED("vivecraft.options.seated", true),
    THEATER("vivecraft_theater_mode.play_mode.theater", true);

    private final String translationKey;
    private final boolean seatedLike;

    TheaterPlayMode(String translationKey, boolean seatedLike) {
        this.translationKey = translationKey;
        this.seatedLike = seatedLike;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public boolean isSeatedLike() {
        return this.seatedLike;
    }

    public TheaterPlayMode next() {
        return switch (this) {
            case STANDING -> SEATED;
            case SEATED -> THEATER;
            case THEATER -> STANDING;
        };
    }

    public static TheaterPlayMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDING;
        }

        for (TheaterPlayMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return STANDING;
    }
}
