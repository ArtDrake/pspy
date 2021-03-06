import java.util.ArrayList;

public class GameUtil {
    
    private final int[][] floor;
    private final int[][] contents;
    
    private final int width;
    private final int height;
    
    public GameUtil (int[][] f, int[][] c) {
        floor = f;
        contents = c;
        width = floor.length;
        height = floor[0].length;
    }
    
    /**
     * Calculates the greatest common denominator of two integers.
     * @param a The first integer.
     * @param b The second integer.
     * @return The greatest d such that a = md and b = nd for some m and n.
     */
    public int gcd (int a, int b) {
        
        a = a > 0 ? a : -a;
        b = b > 0 ? b : -b;
        
        if (a + b == 0) return -1; // GCD is undefined for 0 and 0.
        if (a == 0) return b;
        if (b == 0) return a;
        
        do if (a > b) a %= b; // Whichever one is bigger, divide by the other
            else b %= a;      // and set to the remainder,
        while (a*b > 0);      // until one is equal to zero.
        
        return a == 0 ? b : a;// Then return the one which is not zero.
        
    }
    
    // Displays the character associated with this int code for an object.
    public String displayObjChar (int val) {
        switch (val) {
            case 0: return "&nbsp;";
            case 1: return "<font color = 'white'>O</font>";
            default: switch (val % 2) {
                case 0: return "<font color='red'>O</font>";
                default: return "A";
            }
        }
    }
    
    // Displays the character associated with this int code for a floor tile.
    public String displayFloorChar (int val) {
        switch (val) {
            case 0: return ".";
            case 1: return "#";
            default: return "X";
        }
    }
    
    /** 
     * Returns whether the tile has an entity or other non-ground object on it.
     * (There may be permanent features that are non-floor tiles in the future,
     * which would have object codes but not correspond to an index in the ixAr.
     * These may be given negative codes, or merely low-value reserved codes)
     * @param x the x coordinate
     * @param y the y coordinate
     * @return whether the tile contains an object
     */
    public boolean tileHasObject (int x, int y) {
        if (!IndexUtil.goodCoords(x, y)) return false;
        return contents[IndexUtil.cIx(x)][IndexUtil.cIx(y)] != 0;
    }
    // contents
    
    public boolean tileHasObject (int[] coords) {
        if (coords.length < 2) return false;
        return tileHasObject(coords[0], coords[1]);
    }
    
    /**
     * Expands an array to a set size. Eventually use a sort of ArrayList
     * subclass that does this automagically -- I'd prefer to have an array that
     * lets me add at arbitrary indices. Maybe have a warning go off if I add
     * an extremely large value, or if the density is getting low, then I'll
     * know to refactor the code so it make better use of the space.
     * @param a The array.
     * @param size The size.
     */
    public void expandToSize (ArrayList a, int size) {
        while (a.size() < size) a.add(null);
    }
    
    public GameEntity newEnt (EntType type, int x, int y) {
        switch (type) {
            case ENEMY: return new Enemy(x, y);
            case FURNITURE: return new Furniture(x, y);
            default: return null;
        }
    }
    
    // Returns whether a tile contains a wall.
    // Takes an x and y coordinate.
    public boolean tileClear (int x, int y) {
        if (!IndexUtil.goodCoords(x, y)) return false;
        return floor[IndexUtil.cIx(x)][IndexUtil.cIx(y)] == 0;
    }
    // floor
    
    // Same as above, but takes an array containing the coordinate pair.
    public boolean tileClear (int[] coords) {
        if (coords.length < 2) return false;
        return tileClear(coords[0], coords[1]);
    }
    // none
    
    
    public VisData floorVis (int x, int y) {
        
        x = IndexUtil.cIx(x);
        y = IndexUtil.cIy(y);
        
        int[] distances = new int[4];
        distances[0] = height - y - 1;
        distances[1] = width - x - 1;
        distances[2] = y;
        distances[3] = x;
        // Distances measures the distance from the tile specified, not including
        // the tile itself, to the north, east, south, and west borders, in that order.
        
        Vis[][] ternary = new Vis[width][height];
        double[][] fractional = new double[width][height];
        
        int max = distances[0] + distances[3];
        for (int i = 0; i < 3; i++) max = distances[i] + distances[i+1] > max ? distances[i] + distances[i+1] : max;
        
        ternary[x][y] = Vis.CLEAR;
        boolean[] inBounds = new boolean[4];
        
        for (int radius = 1; radius <= max; radius++) {
            for (int d = 0; d < 4; d++) inBounds[d] = radius <= distances[d];
            if (inBounds[0]) ternary[x][y+radius] = openFloor(ternary, floor, x, y+radius-1)
                    ? Vis.CLEAR : Vis.BLOCKED;
            if (inBounds[1]) ternary[x+radius][y] = openFloor(ternary, floor, x+radius-1, y)
                    ? Vis.CLEAR : Vis.BLOCKED;
            if (inBounds[2]) ternary[x][y-radius] = openFloor(ternary, floor, x, y-radius+1)
                    ? Vis.CLEAR : Vis.BLOCKED;
            if (inBounds[3]) ternary[x-radius][y] = openFloor(ternary, floor, x-radius+1, y)
                    ? Vis.CLEAR : Vis.BLOCKED;
            // Checks the midpoints of each side of the widening square.
            
            int i = 1;
            if (y + radius - i >= height) i = y + radius - height + 1;
            while (i < width - x && i < radius) {
                
                int thisX = x + i, thisY = y + radius - i;
                if (openFloor(ternary, floor, thisX-1, thisY) && openFloor(ternary, floor, thisX, thisY-1))
                    ternary[thisX][thisY] = Vis.CLEAR;
                else if (blockedOff(ternary, floor, thisX-1, thisY) && blockedOff(ternary, floor, thisX, thisY-1))
                    ternary[thisX][thisY] = Vis.BLOCKED;
                else {
                    VisDatum p = GameShell.visLOS(IndexUtil.iCx(x), IndexUtil.iCy(y), IndexUtil.iCx(thisX), IndexUtil.iCy(thisY));
                    ternary[thisX][thisY] = p.v;
                    if (p.v == Vis.PARTIAL) fractional[thisX][thisY] = p.f;
                }
                i++;
            }
            
            i = 1;
            if (y - radius + i < 0) i = radius - y;
            while (i < width - x && i < radius) {
                
                int thisX = x + i, thisY = y - radius + i;
                if (openFloor(ternary, floor, thisX-1, thisY) && openFloor(ternary, floor, thisX, thisY+1))
                    ternary[thisX][thisY] = Vis.CLEAR;
                else if (blockedOff(ternary, floor, thisX-1, thisY) && blockedOff(ternary, floor, thisX, thisY+1))
                    ternary[thisX][thisY] = Vis.BLOCKED;
                else {
                    VisDatum p = GameShell.visLOS(IndexUtil.iCx(x), IndexUtil.iCy(y), IndexUtil.iCx(thisX), IndexUtil.iCy(thisY));
                    ternary[thisX][thisY] = p.v;
                    if (p.v == Vis.PARTIAL) fractional[thisX][thisY] = p.f;
                }
                i++;
                
            }
            
            i = 1;
            if (y - radius + i < 0) i = radius - y;
            while (i <= x && i < radius) {
                
                int thisX = x - i, thisY = y - radius + i;
                if (openFloor(ternary, floor, thisX+1, thisY) && openFloor(ternary, floor, thisX, thisY+1))
                    ternary[thisX][thisY] = Vis.CLEAR;
                else if (blockedOff(ternary, floor, thisX+1, thisY) && blockedOff(ternary, floor, thisX, thisY+1))
                    ternary[thisX][thisY] = Vis.BLOCKED;
                else {
                    VisDatum p = GameShell.visLOS(IndexUtil.iCx(x), IndexUtil.iCy(y), IndexUtil.iCx(thisX), IndexUtil.iCy(thisY));
                    ternary[thisX][thisY] = p.v;
                    if (p.v == Vis.PARTIAL) fractional[thisX][thisY] = p.f;
                }
                i++;
                
            }
            
            i = 1;
            if (y + radius - i >= height) i = y + radius - height + 1;
            while (i <= x && i < radius) {
                
                int thisX = x - i, thisY = y + radius - i;
                if (openFloor(ternary, floor, thisX+1, thisY) && openFloor(ternary, floor, thisX, thisY-1))
                    ternary[thisX][thisY] = Vis.CLEAR;
                else if (blockedOff(ternary, floor, thisX+1, thisY) && blockedOff(ternary, floor, thisX, thisY-1))
                    ternary[thisX][thisY] = Vis.BLOCKED;
                else {
                    VisDatum p = GameShell.visLOS(IndexUtil.iCx(x), IndexUtil.iCy(y), IndexUtil.iCx(thisX), IndexUtil.iCy(thisY));
                    ternary[thisX][thisY] = p.v;
                    if (p.v == Vis.PARTIAL) fractional[thisX][thisY] = p.f;
                }
                i++;
                
            }
            
        }
        
        VisData vd = new VisData(ternary, fractional);
            
        return vd;
        
    }
    
    public boolean openFloor (Vis[][] t, int[][] f, int x, int y) {
        return ((t[x][y] == Vis.CLEAR) && (f[x][y] == 0));
    }
    
    public boolean blockedOff (Vis[][] t, int[][] f, int x, int y) {
        return ((t[x][y] == Vis.BLOCKED) || (f[x][y] == 1));
    }
    
    
}
