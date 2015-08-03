public enum Vis {
    
    CLEAR(true, true),
    BLOCKED(true, false),
    PARTIAL(false, true);
    
    public final boolean absolute;
    public final boolean visible;
    
    private Vis (boolean abs, boolean vis) {
        absolute = abs;
        visible = vis;
    }
    
}
