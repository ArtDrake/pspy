/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Peter Eastwood
 */
public class GameEntity {
    
    private boolean isMob;
    private int health;
    
    private final int[] coords = new int[2];
    
    public GameEntity (int x, int y) {
        coords[0] = x;
        coords[1] = y;
    }
    
    public int[] getCoords () {
        return coords;
    }
    
    public int getX () {
        return coords[0];
    }
    
    public int getY () {
        return coords[1];
    }
    
    public void setCoords (int x, int y) {
        coords[0] = x;
        coords[1] = y;
    }
    
    public void moveCoords (int dx, int dy) {
        coords[0] += dx;
        coords[1] += dy;
    }
    
    public boolean isMob () {
        return isMob;
    }
    
}
