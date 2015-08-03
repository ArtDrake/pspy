public class IndexUtil {
    
    private final int net;
    private final int sx;
    private final int[] ei;
    
    public IndexUtil (int numEntTypes, int startIndex, int[] entIndices) {
        net = numEntTypes;
        sx = startIndex;
        ei = entIndices;
    }
    
    public static int iC (int index) {
        if (index % 2 == 0) return index/2;
        else return -index/2 - 1;
    }
    
    public static int cI (int coord) {
        if (coord < 0) return -coord*2 - 1;
        else return coord*2;
    }
    
    public int typeIndex (EntType type, int i) {
        return sx + type.ix + net*i;
        // A formula for the index of a member of an entity type:
        // start index + type index + number of types * member index.
    }
    
    public int newIndex (EntType type) {
        return net*ei[type.ix] + sx + type.ix;
        // Given a particular type of entity, gives a good index for a new one.
    }
    
    // Ensures that a pair of coordinates is "good" -- within the bounds of the
    // level, and referring to a coordinate actually contained in the array.
    public static boolean goodCoords (int x, int y) {
        return x >= -GameShell.floorXRad 
            && x <=  GameShell.floorXRad 
            && y >= -GameShell.floorYRad 
            && y <=  GameShell.floorYRad;
    }
    
}
