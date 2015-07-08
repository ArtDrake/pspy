/**
 * @author Peter Eastwood
 * 
 * Implementing:
 * Functional floors
 * A JFrame graphical display
 * A controllable player
 * Separate classes for different entities
 * Separated floor and room contents, so things can move around on an unchanging floor.
 * Console controls for the player.
 * Characters for representing the floor, objects, and mobs.
 * A data structure that is expansible and flexible for holding objects at coordinates.
 * Multiple move commands can be given at once.
 * Mobile enemy units.
 * Random enemy movement.
 * Separate loops for console input and actual game turns.
 * Keyboard controls for movement.
 * Locked frame size.
 * Dynamically-sized frame.
 * Entity health or integrity.
 * Ostensibly destructible furniture.
 * A consistent style throughout the code.
 * Full documentation.
 * Methods marked for which static variables they access.
 * Methods physically organized by their role in the program.
 * Generalized entity-spawning code.
 * Naive support for entity removal.
 * Input blocking while turn is in progress.
 * Naive prevention of input buffering.
 * Superior support for entity removal and array cleaning.
 * Boolean line-of-sight.
 * Superior boolean line-of-sight -- realistic. No range.
 * Integrated line-of-sight with the display to reflect it. (No optimization)
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;

public class GameShell implements KeyListener {
    
    public static final Scanner keyboardInput = new Scanner(System.in);
    
    /**
     * The "radius" of the floor in the X direction -- its width in each direction
     * from the center. Always leads to an odd number of tiles.
     */
    public static final int floorXRad = 10;
    /**
     * The "radius" of the floor in the Y direction -- its height in each direction
     * from the center. Always leads to an odd number of tiles' height.
     */
    public static final int floorYRad = 10;
    
    /**
     * The actual width of the floor. Calculated from floorXRad to avoid
     * stupid contradictions.
     */
    private static int floorWidth () {
        return 2*floorXRad + 1;
    }
    
    /**
     * @return The actual height of the floor.
     */
    public static int floorHeight () {
        return 2*floorYRad + 1;
    }
    
    /**
     * Holds all information about the current contents of the floor, including
     * players, enemies, destructible objects, walls, open floor, etc.
     */
    private static final int[][] floor = new int[floorWidth()][floorHeight()];
    private static final int[][] contents = new int[floorWidth()][floorHeight()];
    
    /**
     * Holds the indices of the entities in entities. Serves as an intermediary
     * between the raw tile data and IDs and the entities themselves, and as a
     * sort of lookup table. (ixAr is short for index array)
     */
    private static final ArrayList<Integer> ixAr = new ArrayList<>();
    
    /**
     * Holds the entities themselves in order of spawning in the game.
     * At the moment has no support for removed entities, as this will disrupt
     * the ixAr. Failing to support this will cause memory leakage, while doing
     * so will require integration of ixAr and entities with some new subclass
     * of ArrayList that keeps a child list updated.
     */
    private static final ArrayList<GameEntity> entities = new ArrayList<>();
    
    private static final int numEntTypes = 2;
    private static final int[] entIndices = new int[numEntTypes];
    private static final int startIndex = 2;
    
    private static final ArrayList<Integer> deceased = new ArrayList<>();
    
    // Maybe make a utility that gives ixAr ID based on type and number.
    
    private static GameEntity player = null;
    private static boolean inTurn = false;
    
    /**
     * The "main frame" of the program, holding all graphical information.
     */
    private static final JFrame mainFrame = new JFrame("Psy Spy");
    /**
     * The label that holds all the "graphics" at the moment.
     */
    private static final JLabel mainText = new JLabel("", SwingConstants.CENTER);
    
    // Static block that initializes the frame and label.
    static {
        
        mainText.setFont(new Font("Courier New", Font.PLAIN, 20));
        mainText.setForeground(Color.WHITE);
        
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.getContentPane().setPreferredSize(new Dimension(floorWidth()*24+50, floorHeight()*23+50));
        mainFrame.setResizable(false);
        mainFrame.getContentPane().setBackground(Color.BLACK);
        mainFrame.getContentPane().add(mainText, BorderLayout.CENTER);
        
        mainFrame.setLocationRelativeTo(null);
        mainFrame.pack();
        mainFrame.setVisible(true);
        
    }
    
    private static final GameUtil ug = new GameUtil(floor, contents);
    private static final DrawUtil ud = new DrawUtil(floor, floorWidth(), floorHeight());
    private static final IndexUtil ux = new IndexUtil(numEntTypes, startIndex, entIndices);
    
    ////////////////////////
    // METHODS START HERE //
    ////////////////////////
    
    /**
     * Holds the actual main actions to be performed. Will become sort of
     * superfluous, considering the JFrame works independently and can respond
     * to events.
     * @param args the command line arguments, which I will never use. Ever.
     */
    public static void main (String[] args) {
        
        GameShell shellInstance = new GameShell();
        mainFrame.addKeyListener(shellInstance);
        
        ud.farWalls();
        ud.drawWallVertical(-1, -2, 5);
        ud.drawWallHorizontal(-2, -1, 3);
        ud.drawWallArea(2, 2, 4, 4);
        ud.deleteWall(-1, 2);
        ud.drawWall(6, 6);
        ud.drawWall(6, 8);
        ud.drawWall(8, 6);
        ud.drawWall(8, 8);
        ud.drawWall(7, -7);
        
        ud.drawWallVertical(-8, -5, -6);
        ud.drawWallHorizontal(-8, -8, -7);
        ud.drawWallVertical(-5, -8, -7);
        ud.drawWallHorizontal(-5, -5, -6);
        
        spawnPlayer(0, 0);
        if (player == null) return;
        
        spawnEntity(0, -3, -2);
        spawnEntity(0, 1, 3);
        spawnEntity(0, 1, 4);
        killEntity(4);
        cleanEntities();
        
        spawnEntity(1, 0, 2);
        spawnEntity(1, -2, 1);
        spawnEntity(1, -3, -4);
        
        printFloor();
        
        ug.consoleLoop();
        
        System.exit(0);
        
    }
    
    /**
     * Executes the actions associated with a single in-game turn.
     * @param act A char signifying the player's choice of action on that turn.
     * Some characters may end up prompting the user for further input, like
     * choice of target.
     */
    public static void gameTurn (char act) {
        
        if (!inTurn) {
            
            inTurn = true;
            
            switch (act) {
                case 'i': movePlayer(0, 1); break;
                case 'j': movePlayer(-1, 0); break;
                case 'k': movePlayer(0, -1); break;
                case 'l': movePlayer(1, 0); break;
                case 'r': break;
                default:
            }

            enemiesRandMove();
            printFloor();
            
            /*try {Thread.sleep(15L);
            } catch (InterruptedException e) {
            }*/
            
            inTurn = false;
            
        } else System.out.println("busy");
        
    }
    
    public static void threadTurn (final char act) {
        
        new Thread(new Runnable() {
            @Override
            public void run () {
                gameTurn(act);
            }
        }).start();
        
    }
    
    public static boolean directLOS (int x1, int y1, int x2, int y2) {
        
        if (x1 > x2) {
            int temp = x2;
            x2 = x1;
            x1 = temp;
            temp = y2;
            y2 = y1;
            y1 = temp;
        }
        
        boolean up = y2 > y1;
        
        if (x1 == x2) {
            if (y1 == y2) return true;
            else for (int inc = up?1:-1, i = y1 + inc; i*inc < y2*inc; i += inc)
                if (!ug.tileClear(x1, i)) return false;
            return true;
        } else if (y1 == y2) {
            for (int i = x1 + 1; i < x2; i++)
                if (!ug.tileClear(i, y1)) return false;
            return true;
        } else {
            
            int dx = x2 - x1;
            int dy = y2 - y1;
            int d = dx + (dy > 0 ? dy : -dy);
            int k = ug.gcd(dx, dy);
            
            if (k != 1) {
                for (int i = 0, inf = 0, sup = d/k; i < k; i++) {
                    
                    int ddx = dx/k;
                    int ddy = dy/k;
                    
                    int[] r = utilLOS(x1 + i*ddx, y1 + i*ddy, ddx, ddy, up);
                    if (r == null) return false;
                    
                    inf = r[0] > inf ? r[0] : inf;
                    sup = r[1] < sup ? r[1] : sup;
                    
                    if (sup - inf < 0) return false;
                    
                    if (i != 0 && !ug.tileClear(x1 + i*ddx, y1 + i*ddy)) return false;
                    
                }
                return true;
            } else {
                int[] r = utilLOS(x1, y1, dx, dy, up);
                return r[1] - r[0] >= 0;
            }
            
        }
        
    }
    
    public static double fracLOS (int x1, int y1, int x2, int y2) {
        
        if (x1 > x2) {
            int temp = x2;
            x2 = x1;
            x1 = temp;
            temp = y2;
            y2 = y1;
            y1 = temp;
        }
        
        boolean up = y2 > y1;
        
        if (x1 == x2) {
            if (y1 == y2) return 1d;
            else for (int i = y1 + (up?1:-1); up ? i < y2 : i > y2; i += up ? 1 : -1)
                if (!ug.tileClear(x1, i)) return 0d;
            return 1d;
        } else if (y1 == y2) {
            for (int i = x1 + 1; i < x2; i++)
                if (!ug.tileClear(i, y1)) return 0d;
            return 1d;
        } else {
            
            int dx = x2 - x1;
            int dy = y2 - y1;
            int d = dx + Math.abs(dy);
            int k = ug.gcd(dx, dy);
            
            if (k != 1) {
                
                int inf = 0, sup = d/k;
                
                for (int i = 0; i < k; i++) {
                    
                    int[] r = utilLOS(x1 + i*dx/k, y1 + i*dy/k, dx/k, dy/k, up);
                    if (r == null) return 0d;
                    
                    inf = r[0] > inf ? r[0] : inf;
                    sup = r[1] < sup ? r[1] : sup;
                    
                    if (sup - inf <= 0) return 0d;
                    
                    if (i != 0 && !ug.tileClear(x1 + i*dx/k, y1 + i*dy/k)) return 0d;
                    
                }
                
                return (double)(sup - inf) / (d/k);
                
            } else {
                int[] r = utilLOS(x1, y1, dx, dy, up);
                return r[1] - r[0] > 0 ? (double)(r[1] - r[0]) / d : 0d;
            }
            
        }
        
    }
    // u
    
    public static int[] utilLOS (int x, int y, int dx, int dy, boolean up) {
        
        if (!IndexUtil.goodCoords(x, y) || !IndexUtil.goodCoords(x+dx, y+dy)) return null;
        
        dy = dy > 0 ? dy : -dy;
        int d = dx + dy;
        int sup = d;
        int inf = 0;
        int cur = dx;
        
        if (up) while (true) {
            
            if (!ug.tileClear(x, y+1)) sup = cur < sup ? cur : sup;
            if (!ug.tileClear(x+1, y)) inf = cur > inf ? cur : inf;
            
            if (cur < dy) {
                y++;
                cur += dx;
            } else if (cur > dy) {
                x++;
                cur -= dy;
            } else break;
            
        } else while (true) {
            
            if (!ug.tileClear(x, y-1)) sup = cur < sup ? cur : sup;
            if (!ug.tileClear(x+1, y)) inf = cur > inf ? cur : inf;
            
            if (cur < dy) {
                y--;
                cur += dx;
            } else if (cur > dy) {
                x++;
                cur -= dy;
            } else break;
            
        }
        
        //sup = sup == d ? d : sup + 1;
        // Slight correction in favor of tolerance for seeing around corners.
        // The draconian line-of-sight calculation before was displaying
        // technically accurate but practically weird behaviors.
        
        return new int[]{inf, sup};
        
    }
    // u
    
    /**
     * Randomly moves all enemies. Will be outmoded eventually, and will
     * require fixing after the integration of the various entity indices.
     */
    public static void enemiesRandMove () {
        
        for (int i = 0; i < entIndices[0]; i++) {
            
            int index = i*numEntTypes + startIndex;
            if (ixAr.get(index) == -1) continue;
            
            int rand = (int) (Math.random()*4);
            
            switch (rand) {
                case 0: moveEntity(index, 1, 0); break;
                case 1: moveEntity(index, 0, 1); break;
                case 2: moveEntity(index, -1, 0); break;
                case 3: moveEntity(index, 0, -1); break;
                default:
            }
            
        }
        
    }
    // entIndices, numEntTypes, startIndex, ixAr
    
    /**
     * Moves the player a particular number of spaces horizontally and
     * vertically, relative to its original position. Checks the validity
     * only of the final movement, so this works with teleportation-type commands.
     * @param x The distance to move to the right.
     * @param y The distance to move upwards.
     */
    public static void movePlayer(int x, int y) {
        
        int[] newCoords = new int[]{player.getX(), player.getY()};
        newCoords[0] += x;
        newCoords[1] += y;
        
        if (!ug.tileClear(newCoords)) return;
        if (ug.tileHasObject(newCoords)) return;
        
        contents[IndexUtil.cI(player.getX())][IndexUtil.cI(player.getY())] = 0;
        contents[IndexUtil.cI(newCoords[0])][IndexUtil.cI(newCoords[1])] = 1;
        player.moveCoords(x, y);
        
    }
    // player, contents
    
    /**
     * Moves a generic entity in the same fashion as movePlayer -- adds
     * a certain vector <x, y> to the entity's position. 
     * @param id The position of the entity in ixAr. NOT the entity's index in
     * entities.
     * @param x The distance moved to the right.
     * @param y The distance moved upwards.
     */
    public static void moveEntity (int id, int x, int y) {
        
        GameEntity entity = entities.get(ixAr.get(id));
        
        if (entity == null) {
            System.out.println(id);
            System.out.println(ixAr.get(id));
            return;
        }
        
        int[] newCoords = new int[]{entity.getX(), entity.getY()};
        newCoords[0] += x;
        newCoords[1] += y;
        
        if (!ug.tileClear(newCoords)) return;
        if (ug.tileHasObject(newCoords)) return;
        
        contents[IndexUtil.cI(entity.getX())][IndexUtil.cI(entity.getY())] = 0;
        contents[IndexUtil.cI(newCoords[0])][IndexUtil.cI(newCoords[1])] = id;
        
        entity.moveCoords(x, y);
        
    }
    // Entities, ixAr, contents, u
    
    /**
     * Removes an entity, replacing its entries in ixAr and entities
     * with -1 and null, respectively.
     * @param id 
     */
    public static void killEntity (int id) {
        
        int ix = ixAr.get(id);
        GameEntity entity = entities.get(ix);
        
        contents[IndexUtil.cI(entity.getX())][IndexUtil.cI(entity.getY())] = 0;
        entities.set(ix, null);
        deceased.add(ix);
        ixAr.set(id, -1);
        
    }
    // ixAr, entities, contents, deceased
    
    public static void cleanEntities () {
        
        for (int i = 0; i < numEntTypes; i++) {
            int k = 0;
            for (int j = 0; j < entIndices[i]; j++) {
                if (ixAr.get(ux.typeIndex(i, j)) == -1) {
                    k++;
                } else if (k > 0) {
                    ixAr.set(ux.typeIndex(i, j-k), ixAr.get(ux.typeIndex(i, j)));
                }
            }
            for (int j = entIndices[i] - k; j < entIndices[i]; j++) ixAr.set(ux.typeIndex(i, j), null);
            entIndices[i] -= k;
            
            for (int j = 0; j < entIndices[i]; j++) {
                int ix = ux.typeIndex(i, j);
                GameEntity e = entities.get(ixAr.get(ix));
                contents[IndexUtil.cI(e.getX())][IndexUtil.cI(e.getY())] = ix;
            }
            
        }
        
    }
    // numEntTypes, entIndices, ixAr, ux, entities, contents
    
    /**
     * Generates an HTML representation of the floor and transfers it to mainText.
     */
    public static void printFloor () {
        
        String labelOutput = "<html>";
        
        for (int y = floorYRad; y >= -floorYRad; y--) {
            
            for (int x = -floorXRad; x <= floorXRad; x++) {
                
                String shown;
                if (!directLOS(player.getX(), player.getY(), x, y)) shown = "&nbsp";
                
                else {
                    
                    shown = ug.tileHasObject(x, y) 
                            ? ug.displayObjChar(contents[IndexUtil.cI(x)][IndexUtil.cI(y)])
                            : ug.displayFloorChar(floor[IndexUtil.cI(x)][IndexUtil.cI(y)]);
                    
                    String digit;
                    {   double visibility = fracLOS(player.getX(), player.getY(), x, y);
                        int vis = (int) (visibility * 255);
                        if (vis == 0) vis = 45;
                        digit = Integer.toHexString(vis);
                        if (digit.length() == 1) digit = "0" + digit;
                        digit = digit + digit + digit;
                    }
                    
                    shown = "<font color='" + digit + "'>" + shown + "</font>";
                    
                }
                
                String inBetween = x == floorXRad ? "" : "&nbsp;";
                
                labelOutput += shown + inBetween;
                
            }
            
            labelOutput += y == -floorYRad ? "" : "<br>";
            
        }
        
        labelOutput += "</html>";
        
        mainText.setText(labelOutput);
    }
    // FloorXRad, floorYRad, contents, floor, maintext
    
    /**
     * Spawns the player entity.
     * @param x X coordinate.
     * @param y Y coordinate.
     */
    public static void spawnPlayer (int x, int y) {
        
        if (ug.tileClear(x, y) && !ug.tileHasObject(x, y)) {
            
            contents[IndexUtil.cI(x)][IndexUtil.cI(y)] = 1;
            ug.expandToSize(ixAr, 2);
            ixAr.set(1, entities.size());
            
            player = new Player(x, y);
            entities.add(player);
            
        }
        
    }
    // contents, ixAr, entities, player, u
    
    public static void spawnEntity (int type, int x, int y) {
        
        if (type >= numEntTypes) {
            System.out.println("Called with bad type");
            return;
        }
        
        if (ug.tileClear(x, y) && !ug.tileHasObject(x, y)) {
            
            int index = ux.newIndex(type);
            contents[IndexUtil.cI(x)][IndexUtil.cI(y)] = index;
            ug.expandToSize(ixAr, index+1);
            
            if (deceased.size() > 0) {
                int ix = deceased.get(0);
                ixAr.set(index, ix);
                entities.set(ix, ug.newEnt(type, x, y));
                deceased.remove(0);
            } else {
                ixAr.set(index, entities.size());
                entities.add(ug.newEnt(type, x, y));
            }
            
            entIndices[type]++;
            
        }
        
    }
    // numEntTypes, contents, ixAr, deceased, entities, entIndices, u
    
    /**
     * Does nothing, but must be overridden for this class to extend KeyListener.
     * @param e The KeyEvent that triggers this function.
     */
    @Override
    public void keyTyped (KeyEvent e) {
        
    }
    
    /**
     * Handles all the responses to keyPresses. Will eventually be replaced
     * with a better system that does not equate events with turns. This is a disaster
     * waiting to happen.
     * @param e The KeyEvent that triggers this function.
     */
    @Override
    public void keyPressed (KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case 37: threadTurn('j'); break;
            case 38: threadTurn('i'); break;
            case 39: threadTurn('l'); break;
            case 40: threadTurn('k'); break;
            case 46: threadTurn('r'); break;
            default:
        }
    }
    // none
    
    // Same as keyTyped.
    @Override
    public void keyReleased (KeyEvent e) {
        
    }
    
}
