package dev.justfeli.vtm.client.environment;

import java.util.ArrayList;
import java.util.List;

final class TheaterEnvironmentConfig {
    public float[] playerPosition = new float[]{0.0F, 1.6F, 0.0F};
    public float[] playerRotation = new float[]{0.0F, 0.0F, 0.0F};
    public ScreenConfig screen = new ScreenConfig();
    public List<ObjectConfig> objects = new ArrayList<>();

    static final class ScreenConfig {
        public float[] position = new float[]{0.0F, 1.4F, -2.5F};
        public float[] rotation = new float[]{0.0F, 0.0F, 0.0F};
        public float[] size = new float[]{1.8F, 1.0F};
    }

    static final class ObjectConfig {
        public String model;
        public String texture;
        public boolean translucent = false;
        public float[] position = new float[]{0.0F, 0.0F, 0.0F};
        public float[] rotation = new float[]{0.0F, 0.0F, 0.0F};
        public float[] scale = new float[]{1.0F, 1.0F, 1.0F};
    }
}
