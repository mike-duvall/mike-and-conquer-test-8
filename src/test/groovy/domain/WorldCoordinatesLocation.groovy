package domain

class WorldCoordinatesLocation {

    static int MAP_TILE_WIDTH = 24


    private int x
    private int y

    private WorldCoordinatesLocation(int x, int y) {
        this.x = x
        this.y = y

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

    static WorldCoordinatesLocation CreatFromWorldCoordinates(int x, int y) {
        WorldCoordinatesLocation location = new WorldCoordinatesLocation(x,y)
        return location
    }

    static WorldCoordinatesLocation CreatFromWorldMapTileCoordinates(int x, int y) {
        int halfMapSquareWidth = MAP_TILE_WIDTH / 2
        int worldX = (x * MAP_TILE_WIDTH) + halfMapSquareWidth
        int worldY = (y * MAP_TILE_WIDTH) + halfMapSquareWidth

        WorldCoordinatesLocation location = new WorldCoordinatesLocation(worldX, worldY)
        return location
    }

}
