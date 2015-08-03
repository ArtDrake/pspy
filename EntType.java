public enum EntType {
    
    ENEMY(0),
    FURNITURE(1);
    
    public final int ix;
    
    private EntType (int typeIndex) {
        ix = typeIndex;
    }
    
}
