public class VisDatum {
    
    public static final VisDatum CLEAR = new VisDatum(Vis.CLEAR, null);
    public static final VisDatum BLOCKED = new VisDatum(Vis.BLOCKED, null);
    
    public Vis v;
    public Double f;
    
    public VisDatum (Vis vis, Double frac) {
        v = vis;
        f = frac;
    }
    
}
