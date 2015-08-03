import java.util.ArrayList;

public class GameUtil {
    
    private final int[][] floor;
    private final int[][] contents;
    
    public GameUtil (int[][] floor, int[][] contents) {
        this.floor = floor;
        this.contents = contents;
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
        return contents[IndexUtil.cI(x)][IndexUtil.cI(y)] != 0;
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
        return floor[IndexUtil.cI(x)][IndexUtil.cI(y)] == 0;
    }
    // floor
    
    // Same as above, but takes an array containing the coordinate pair.
    public boolean tileClear (int[] coords) {
        if (coords.length < 2) return false;
        return tileClear(coords[0], coords[1]);
    }
    // none
    
}
