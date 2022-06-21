package domain

class WorldCoordinatesLocation {

    static int MAP_TILE_WIDTH = 24


    private int x
    private int y

    private WorldCoordinatesLocation(int x, int y) {
        this.x = x
        this.y = y
    }

    public WorldCoordinatesLocation(WorldCoordinatesLocationBuilder builder) {
        this.x = builder.worldCoordinatesX
        this.y = builder.worldCoordinatesY
    }

    int XInWorldCoordinates() {
        return x
    }

    int YInWorldCoordinates() {
        return y
    }

    int XInWorldMapTileCoordinates() {
        return x / MAP_TILE_WIDTH
    }

    int YInWorldMapTileCoordinates() {
        return y / MAP_TILE_WIDTH
    }


}
