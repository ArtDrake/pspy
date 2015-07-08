
public class DrawUtil {
    
    private final int[][] floor;
    private final int w;
    private final int h;
    
    private int brush = 1;
    
    public DrawUtil (int[][] floor, int width, int height) {
        this.floor = floor;
        w = width;
        h = height;
    }
    
    
    /**
     * Generates the outer (permanent) walls of the level. Most other
     * walls will eventually not be of this type, because these are to prevent
     * the player from leaving the level and running into illegal spaces.
     */
    public void farWalls () {
        
        // Top and bottom sides plus corners
        for (int i = 0; i < w; i++) {
            floor[i][h-1] = brush;
            floor[i][h-2] = brush;
        }
        
        // Left and right sides minus corners
        for (int i = 0; i < h-2; i++) {
            floor[w-1][i] = brush;
            floor[w-2][i] = brush;
        }
        
    }
    
    public void drawWall (int x, int y) {
        
        if (!IndexUtil.goodCoords(x, y)) return;
        floor[IndexUtil.cI(x)][IndexUtil.cI(y)] = brush;
        
    }
    
    public void drawWallVertical (int x, int end1, int end2) {
        
        if (!IndexUtil.goodCoords(x, end1) || !IndexUtil.goodCoords(x, end2)) return;
        
        if (end1 > end2) {
            int temp = end1;
            end1 = end2;
            end2 = temp;
        }
        
        for (int i = end1; i <= end2; i++) {
            floor[IndexUtil.cI(x)][IndexUtil.cI(i)] = brush;
        }
        
    }
    
    public void drawWallHorizontal (int y, int end1, int end2) {
        
        if (!IndexUtil.goodCoords(end1, y) || !IndexUtil.goodCoords(end2, y)) return;
        
        if (end1 > end2) {
            int temp = end1;
            end1 = end2;
            end2 = temp;
        }
        
        for (int i = end1; i <= end2; i++) {
            floor[IndexUtil.cI(i)][IndexUtil.cI(y)] = brush;
        }
        
    }
    
    /**
     * Draws a rectangle of permanent wall with (x1, y1) and (x2, y2) as
     * opposite corners.
     * @param x1
     * @param y1
     * @param x2
     * @param y2 
     */
    public void drawWallArea (int x1, int y1, int x2, int y2) {
        
        if (!IndexUtil.goodCoords(x1, y1) || !IndexUtil.goodCoords(x2, y2)) return;
        
        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        
        if (y1 > y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }
        
        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                floor[IndexUtil.cI(i)][IndexUtil.cI(j)] = brush;
            }
        }
        
    }
    
    public void drawRoom (int x1, int y1, int x2, int y2) {
        
        if (!IndexUtil.goodCoords(x1, y1) || !IndexUtil.goodCoords(x2, y2)) return;
        
        drawWallVertical(x1, y1, y2);
        drawWallVertical(x2, y1, y2);
        drawWallHorizontal(y1, x1, x2);
        drawWallHorizontal(y2, x1, x2);
        
    }
    
    public void deleteWall (int x, int y) {
        
        if (!IndexUtil.goodCoords(x, y)) return;
        floor[IndexUtil.cI(x)][IndexUtil.cI(y)] = 0;
        
    }
    
    public void deleteWallVertical (int x, int y1, int y2) {
        
        int temp = brush;
        brush = 0;
        drawWallVertical(x, y1, y2);
        brush = temp;
        
    }
    
    public void deleteWallHorizontal (int y, int x1, int x2) {
        
        int temp = brush;
        brush = 0;
        drawWallVertical(y, x1, x2);
        brush = temp;
        
    }
    
    public void deleteWallArea (int x1, int y1, int x2, int y2) {
        
        int temp = brush;
        brush = 0;
        drawWallArea(x1, y1, x2, y2);
        brush = temp;
        
    }
    
    public void changeBrush (int newBrush) {
        brush = newBrush;
    }
    
}
