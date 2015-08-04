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
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final int floorWidth = 2*floorXRad + 1;
    
    /**
     * The actual height of the floor.
     */
    public static final int floorHeight = 2*floorYRad + 1;
    
    /**
     * Holds all information about the current contents of the floor, including
     * players, enemies, destructible objects, walls, open floor, etc.
     * Floor holds the floor layer's contents left-to-right and bottom-to-top.
     * This is a major improvement over the last nonsense system.
     * 
     * Contents does the same thing, but things on the contents level are fundamentally
     * separate from things on the floor level -- they sit on top of the floor, and
     * coexist with what's below, and in some cases interact with it; permawalls,
     * which at this point are considered part of the floor, block movement of
     * entities, which are universally on the contents level at this time.
     */
    private static final int[][] floor = new int[floorWidth][floorHeight];
    private static final int[][] contents = new int[floorWidth][floorHeight];
    
    /**
     * Holds the indices of the entities in entities. Serves as an intermediary
     * between the raw tile data and IDs and the entities themselves, and as a
     * sort of lookup table. (ixAr is short for index array)
     * 
     * This one is a little harder to explain. In contents, which is an int 2D
     * array, numbers represent the entities. These numbers refer to the positions
     * of entities in ixAr. Once the numbers have been used to find the entity's
     * associated number here, the result is the index of the entity in the less
     * organized, more all-inclusive array called 'entities'.
     * 
     * The entities themselves all have coordinates, so they can refer back to
     * contents if necessary. Contents -> ixAr -> entities -> the entity -> contents.
     * I'd propose that the entities themselves have their own ixAr IDs.
     */
    private static final ArrayList<Integer> ixAr = new ArrayList<>();
    
    /**
     * Holds the entities themselves in order of spawning in the game.
     * At the moment has no support for removed entities, as this will disrupt
     * the ixAr. Failing to support this will cause memory leakage, while doing
     * so will require integration of ixAr and entities with some new subclass
     * of ArrayList that keeps a child list updated.
     * 
     * The position of an entity in entities has no necessary correlation with
     * its position in ixAr, so removing an entity from entities would have to
     * consist of telling each one where to tell ixAr it now resides.
     * 
     * This is one of those tasks that it makes more sense to perform occasionally
     * rather than constantly. I should figure out how often.
     */
    private static final ArrayList<GameEntity> entities = new ArrayList<>();
    
    /**
     * The number of distinct types of entities. This one is also a little
     * weird. IxAr is organized. It's prepared for any quantity of any type of
     * entity, but adds this space dynamically, on an as-needed basis.
     * It does so, after a few reserved slots for special entities like the player,
     * by taking the number of entity types n, and placing new entities of a given
     * type every n indices, while leaving the n-1 spaces in between empty for
     * any other entities that need to be recorded.
     * It's technically not the most space-efficient method, but allows for quick
     * and simple manipulation of entity-related data on a turn-by-turn basis --
     * do a simple mod test for entity type, shift entities of a certain type into
     * the place formerly taken by a deleted one by reducing their indices all by
     * a set quantity (n). Access all entities of a certain type by index based
     * only on how many there are. Etc.
     * 
     * It's not perfect -- there are arguments for forgoing this organizational
     * system and abolishing ixAr.
     */
    private static final int numEntTypes = 2;
    
    /**
     * The numbers of indices held by each entity type.
     * If this reads 2, 5, there are two Enemy entities and five Furniture entities.
     * However, it's phrased in terms of indices held because it counts indices
     * still held after death or destruction or other deletion. I.e., as yet to
     * be cleaned up.
     */
    private static final int[] entIndices = new int[numEntTypes];
    
    /**
     * The number of indices held by special entities.
     * Correspondingly, the starting point for the rest of the indices.
     */
    private static final int startIndex = 2;
    
    /**
     * Keeps track of indices in entities freed by killing their occupants.
     * Contrary to the name, deceased doesn't keep track of the indices of the
     * deceased any longer than it takes to give them to something else. This
     * partially counteracts the effects of the lack of cleanup that happens in
     * the entities list. I reuse, but don't recycle, so to speak.
     */
    private static final ArrayList<Integer> deceased = new ArrayList<>();
    
    private static GameEntity player = null;
    // Initialize this so that if the player spawning fails, the null comparison
    // can be made that'll shut the program down.
    
    private static boolean inTurn = false;
    // This is for preventing multiple turns from happening simultaneously -- each
    // one takes a nonzero amount of time, and sometimes there are spikes. If the
    // user is holding down a key, they might try to start a turn before the last
    // one finished.
    
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
        // White text
        
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.getContentPane().setPreferredSize(new Dimension(floorWidth*24+50, floorHeight*23+50));
        mainFrame.setResizable(false);
        // I don't have enough control over Swing to make resizing an attractive
        // option.
        mainFrame.getContentPane().setBackground(Color.BLACK);
        // On a black background
        mainFrame.getContentPane().add(mainText, BorderLayout.CENTER);
        
        mainFrame.setLocationRelativeTo(null);
        mainFrame.pack();
        mainFrame.setVisible(true);
        
    }
    
    private static final GameUtil ug = new GameUtil(floor, contents);
    private static final DrawUtil ud = new DrawUtil(floor, floorWidth, floorHeight);
    private static final IndexUtil ux = new IndexUtil(numEntTypes, startIndex, entIndices);
    
    // These are instances of utility classes that I'm passing mostly private static
    // variables -- I don't want just anyone to be able to access these critical
    // data structures, but I do want to delegate some methods that deal mainly
    // with specific lists over to other classes. The bloat is already real.
    
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
        
        spawnEntity(EntType.ENEMY, -3, -2);
        spawnEntity(EntType.ENEMY, 1, 3);
        spawnEntity(EntType.ENEMY, 1, 4);
        killEntity(4);
        cleanEntities();
        
        // SpawnEntity's arguments are entity type and coordinates.
        
        spawnEntity(EntType.FURNITURE, 0, 2);
        spawnEntity(EntType.FURNITURE, -2, 1);
        spawnEntity(EntType.FURNITURE, -3, -4);
        
        shellInstance.printFloor();
        
        while (true);
        
        //System.exit(0);
        
    }
    
    public GameShell () {
        
        //printFloor();
    }
    
    /**
     * Executes the actions associated with a single in-game turn.
     * @param act A char signifying the player's choice of action on that turn.
     * Some characters may end up prompting the user for further input, like
     * choice of target.
     */
    public void gameTurn (Move act) {
        
        if (!inTurn) {
            
            inTurn = true;
            
            switch (act) {
                case UP: movePlayer(0, 1); break;
                case LEFT: movePlayer(-1, 0); break;
                case DOWN: movePlayer(0, -1); break;
                case RIGHT: movePlayer(1, 0); break;
                case WAIT: break;
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
    
    /**
     * Puts the gameTurn method onto a new thread.
     * This is intended to make sure the measure I put in place to prevent
     * concurrent turns actually works properly. If I don't use this, despite
     * the fact that events are technically all on distinct threads, the turns
     * can start to overlap. Not quite sure why.
     * @param act 
     */
    public void threadTurn (final Move act) {
        
        new Thread(new Runnable() {
            @Override
            public void run () {
                gameTurn(act);
            }
        }).start();
        
    }
    
    /**
     * Determines whether or not two squares can see another clearly,
     * partially, or not at all.
     * Specifically, the algorithm determines whether there exists a point in one
     * tile that has line-of-sight to its counterpart in the second tile, and if
     * so, whether every point can do so. This is not quite the same as determining
     * whether there are any points in the two tiles that can 'see' one another
     * at all, but it's close enough, and it doesn't treat diagonals unfairly.
     * @param x1 
     * @param y1
     * @param x2
     * @param y2
     * @return 
     */
    public static Vis directLOS (int x1, int y1, int x2, int y2) {
        
        if (x1 > x2) {
            int temp = x2;
            x2 = x1;
            x1 = temp;
            temp = y2;
            y2 = y1;
            y1 = temp;
        }
        
        // Switches the coordinate pairs so that p1 is on the left.
        
        boolean up = y2 > y1;
        // Whether p2 is above p1.
        
        if (x1 == x2) {
            if (y1 == y2) return Vis.CLEAR;
            else for (int inc = up?1:-1, i = y1 + inc; i*inc < y2*inc; i += inc)
                if (!ug.tileClear(x1, i)) return Vis.BLOCKED;
            return Vis.CLEAR;
        } else if (y1 == y2) {
            for (int i = x1 + 1; i < x2; i++)
                if (!ug.tileClear(i, y1)) return Vis.BLOCKED;
            return Vis.CLEAR;
        // Those were the easy conditions -- if the tiles are horizontally or
        // vertically aligned with one another, or if they're in fact the same
        // tile, the preceding conditions will catch that and run simpler tests.
        } else {
            
            int dx = x2 - x1;                   // The horizontal distance.
            int dy = y2 - y1;                   // The vertical distance (negative if p2 is below)
            int d = dx + (dy > 0 ? dy : -dy);   // The sum (with absolute value of dy)
            int k = ug.gcd(dx, dy);
            
            // I'm using the gcd to determine how many tiles are precisely
            // centered on the line between the two being tested.
            
            // The k!=1 section basically cuts up the test into several smaller
            // tests, and checks the interceding tiles that are right on the line.
            // (not to mention, ends the test prematurely line-of-sight is seen
            //  to be blocked without testing every subtest)
            // Then it compiles the results into a single bigger result, and
            // does the same thing with it that the k==1 section does.
            
            // The k==1 section just does the straightforward but intensive version
            // of the test, because there's no way around it. Dx and dy are
            // relatively prime.
            
            if (k != 1) {
                int inf = 0, sup = d/k;
                for (int i = 0; i < k; i++) {
                    
                    int ddx = dx/k; // smaller horiz. length
                    int ddy = dy/k; // smaller vert.  length
                    
                    int[] r = utilLOS(x1 + i*ddx, y1 + i*ddy, ddx, ddy, up);
                    if (r == null) return Vis.BLOCKED; // Just in case.
                    
                    inf = r[0] > inf ? r[0] : inf; // Greatest lower bound on occlusion.
                    sup = r[1] < sup ? r[1] : sup; // Least upper bound, occlusion.
                    
                    if (sup - inf < 0) return Vis.BLOCKED;
                    // If the least upper bound is less than the greatest lower bound,
                    // that means the interval of line-of-sight doesn't exist.
                    
                    // I made it so there had to be less than no way to see through,
                    // so corners would be visible.
                    
                    if (i != 0 && !ug.tileClear(x1 + i*ddx, y1 + i*ddy)) return Vis.BLOCKED;
                    // If one of the interceding tiles is standing square in the way,
                    // line-of-sight is blocked by it, regardless of the rest.
                    
                }
                return (sup - inf == d/k) ? Vis.CLEAR : Vis.PARTIAL;
                // If LOS is not confirmed blocked, it's some form of clear.
            } else {
                int[] r = utilLOS(x1, y1, dx, dy, up);
                int aperture = r[1] - r[0];
                return (aperture >= 0) ? ((aperture == d) ? Vis.CLEAR : Vis.PARTIAL) : Vis.BLOCKED;
                // Essentially sup - inf.
            }
            
        }
        
    }
    
    // Pretty much the same thing, but with fractions.
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
    
    public static VisDatum visLOS (int x1, int y1, int x2, int y2) {
        
        if (x1 > x2) {
            int temp = x2;
            x2 = x1;
            x1 = temp;
            temp = y2;
            y2 = y1;
            y1 = temp;
        }
        
        boolean up = y2 > y1;
        
        int dx = x2 - x1;                   // The horizontal distance.
        int dy = y2 - y1;                   // The vertical distance (negative if p2 is below)
        int d = dx + (dy > 0 ? dy : -dy);   // The sum (with absolute value of dy)
        int k = ug.gcd(dx, dy);
        
        if (k != 1) {
            int inf = 0, sup = d/k;
            for (int i = 0; i < k; i++) {

                int ddx = dx/k; // smaller horiz. length
                int ddy = dy/k; // smaller vert.  length

                int[] r = utilLOS(x1 + i*ddx, y1 + i*ddy, ddx, ddy, up);
                if (r == null) return VisDatum.BLOCKED; // Just in case.

                inf = r[0] > inf ? r[0] : inf; // Greatest lower bound on occlusion.
                sup = r[1] < sup ? r[1] : sup; // Least upper bound, occlusion.

                if (sup - inf < 0) return VisDatum.BLOCKED;
                // If the least upper bound is less than the greatest lower bound,
                // that means the interval of line-of-sight doesn't exist.

                // I made it so there had to be less than no way to see through,
                // so corners would be visible.

                if (i != 0 && !ug.tileClear(x1 + i*ddx, y1 + i*ddy)) return VisDatum.BLOCKED;
                // If one of the interceding tiles is standing square in the way,
                // line-of-sight is blocked by it, regardless of the rest.

            }
            return (sup - inf == d/k) ? VisDatum.CLEAR : new VisDatum(Vis.PARTIAL, (double)(sup - inf) / (d/k));
            // If LOS is not confirmed blocked, it's some form of clear.
        } else {
            int[] r = utilLOS(x1, y1, dx, dy, up);
            //System.out.println(Arrays.toString(new int[]{x1, y1, dx, dy}));
            int aperture = r[1] - r[0];
            return (aperture >= 0) ? ((aperture == d) ? VisDatum.CLEAR : new VisDatum(Vis.PARTIAL, (double)(r[1] - r[0]) / d)) : VisDatum.BLOCKED;
            // Essentially sup - inf.
        }
        
    }
    
    // Blah blah number theory.
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
        
        contents[IndexUtil.cIx(player.getX())][IndexUtil.cIy(player.getY())] = 0;
        contents[IndexUtil.cIx(newCoords[0])][IndexUtil.cIy(newCoords[1])] = 1;
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
        // Check that the destination is clear.
        
        contents[IndexUtil.cIx(entity.getX())][IndexUtil.cIy(entity.getY())] = 0;
        contents[IndexUtil.cIx(newCoords[0])][IndexUtil.cIy(newCoords[1])] = id;
        
        entity.moveCoords(x, y);
        
    }
    // Entities, ixAr, contents, u
    
    /**
     * Removes an entity, replacing its entries in ixAr and entities
     * with -1 and null, respectively.
     * @param id 
     */
    public static void killEntity (int id) {
        
        int ix = ixAr.get(id);                  // Index in entities
        GameEntity entity = entities.get(ix);   // Retrieve the entity itself
        
        contents[IndexUtil.cIx(entity.getX())][IndexUtil.cIy(entity.getY())] = 0;
        // Empty the tile.
        entities.set(ix, null);                 // Empty its index.
        deceased.add(ix);                       // Add it to the deceased.
        ixAr.set(id, -1);                       // Mark it as deceased in ixAr.
        
    }
    // ixAr, entities, contents, deceased
    
    public static void cleanEntities () {
        
        for (EntType type : EntType.values()) {             // Iterate through types.
            int k = 0;                                      // Counter for deceased.
            int index = type.ix;                            // Index of type.
            {   int ei = entIndices[index];                 // Held indices of this type.
                for (int j = 0; j < ei; j++) {              // Iterate through held indices.
                    if (ixAr.get(ux.typeIndex(type, j)) == -1) {
                        k++;
                    } else if (k > 0) {
                        ixAr.set(ux.typeIndex(type, j-k), ixAr.get(ux.typeIndex(type, j)));
                        // Shift them down.
                    }
                }
                for (int j = ei - k; j < ei; j++) ixAr.set(ux.typeIndex(type, j), null);
                // Null for unused, instead of simply deceased.
            }
            entIndices[index] -= k;
            // The deceased no longer hold their indices.
            
            for (int j = 0; j < entIndices[index]; j++) {
                int ix = ux.typeIndex(type, j);
                GameEntity e = entities.get(ixAr.get(ix));
                contents[IndexUtil.cIx(e.getX())][IndexUtil.cIy(e.getY())] = ix;
            }
            // Update the contents array with the new indices of the living.
            
        }
        
    }
    // numEntTypes, entIndices, ixAr, ux, entities, contents
    
    /**
     * Generates an HTML representation of the floor and transfers it to mainText.
     * Don't pay too much attention to this method -- it's sloppy and I'd like
     * to get rid of this display type as soon as possible. I just haven't found
     * an acceptable substitute yet.
     */
    public void printFloor () {
        
        VisData vd = ug.floorVis(player.getX(), player.getY());
        Vis[][] t = vd.ternary;
        double[][] f = vd.fractional;
        
        String labelOutput = "<html>";
        
        for (int y = floorHeight-1; y > -1; y--) {
            for (int x = 0; x < floorWidth; x++) {
                String shown;
                Vis v = t[x][y];
                switch (v) {
                    case BLOCKED:
                        shown = "&nbsp;";
                        break;
                    case CLEAR:
                    case PARTIAL:
                        shown = ug.tileHasObject(IndexUtil.iCx(x), IndexUtil.iCy(y)) 
                                ? ug.displayObjChar(contents[x][y])
                                : ug.displayFloorChar(floor[x][y]);
                        if (v == Vis.CLEAR) break;
                        String digit;
                        {   double visibility = f[x][y];
                            int vis = (int) (visibility * 255);
                            digit = Integer.toHexString(vis);
                            if (digit.length() == 1) digit = "0" + digit;
                            digit = digit + digit + digit;
                        }
                        shown = "<font color='" + digit + "'>" + shown + "</font>";
                        break;
                    default: shown = "ERROR";
                }
                String inBetween = x == floorWidth-1 ? "" : "&nbsp;";
                labelOutput += shown + inBetween;
            }
            labelOutput += y == 0 ? "" : "<br>";
        }
        
        /*for (int y = floorYRad; y >= -floorYRad; y--) {
            
            for (int x = -floorXRad; x <= floorXRad; x++) {
                
                String shown;
                Vis v = directLOS(player.getX(), player.getY(), x, y);
                if (v == Vis.BLOCKED) shown = "&nbsp";
                else if (v == Vis.CLEAR) shown = ug.tileHasObject(x, y) 
                        ? ug.displayObjChar(contents[IndexUtil.cIx(x)][IndexUtil.cIy(y)])
                        : ug.displayFloorChar(floor[IndexUtil.cIx(x)][IndexUtil.cIy(y)]);
                
                else {
                    
                    shown = ug.tileHasObject(x, y) 
                            ? ug.displayObjChar(contents[IndexUtil.cIx(x)][IndexUtil.cIy(y)])
                            : ug.displayFloorChar(floor[IndexUtil.cIx(x)][IndexUtil.cIy(y)]);
                    
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
        */
        
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
            
            contents[IndexUtil.cIx(x)][IndexUtil.cIy(y)] = 1;
            ug.expandToSize(ixAr, 2);
            ixAr.set(1, entities.size());
            
            player = new Player(x, y);
            entities.add(player);
            
        }
        
    }
    // contents, ixAr, entities, player, u
    
    public static void spawnEntity (EntType type, int x, int y) {
        
        if (ug.tileClear(x, y) && !ug.tileHasObject(x, y)) {
        // Tile being spawned into must be clear of permawalls and entities.
            
            int index = ux.newIndex(type);
            contents[IndexUtil.cIx(x)][IndexUtil.cIy(y)] = index;
            ug.expandToSize(ixAr, index+1);
            // Make sure ixAr is big enough for the incoming entity.
            
            if (deceased.size() > 0) {
                int ix = deceased.get(0);
                ixAr.set(index, ix);
                entities.set(ix, ug.newEnt(type, x, y));
                deceased.remove(0);
            } else {
                ixAr.set(index, entities.size());
                entities.add(ug.newEnt(type, x, y));
            }
            // Use deceased indices if possible.
            
            entIndices[type.ix]++;
            
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
        if (inTurn) return;
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case 37: threadTurn(Move.LEFT); break;
            case 38: threadTurn(Move.UP); break;
            case 39: threadTurn(Move.RIGHT); break;
            case 40: threadTurn(Move.DOWN); break;
            case 46: threadTurn(Move.WAIT); break;
            default:
        }
    }
    // none
    
    // Same as keyTyped.
    @Override
    public void keyReleased (KeyEvent e) {
        
    }
    
}
