package domain

class WorldCoordinatesLocationBuilder {


    private int worldCoordinatesX
    private int worldCoordinatesY

    static int MAP_TILE_WIDTH = 24
    static int halfMapSquareWidth = MAP_TILE_WIDTH / 2

    WorldCoordinatesLocationBuilder worldMapTileCoordinatesX(int x) {
        this.worldCoordinatesX = (x * MAP_TILE_WIDTH) + halfMapSquareWidth
        return this
    }

    WorldCoordinatesLocationBuilder worldMapTileCoordinatesY(int y) {
        this.worldCoordinatesY = (y * MAP_TILE_WIDTH) + halfMapSquareWidth
        return this
    }

    WorldCoordinatesLocationBuilder worldCoordinatesX(int x) {
        this.worldCoordinatesX = x
        return this
    }

    WorldCoordinatesLocationBuilder worldCoordinatesY(int y) {
        this.worldCoordinatesY = y
        return this
    }


    public WorldCoordinatesLocation build() {
        WorldCoordinatesLocation worldCoordinatesLocation = new WorldCoordinatesLocation(this)
        return worldCoordinatesLocation
    }
}
