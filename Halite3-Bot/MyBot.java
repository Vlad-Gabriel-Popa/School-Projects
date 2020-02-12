// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.*;
import java.util.Random;

public class MyBot {

    //setul retine navele care se intorc la baza
    static Set<EntityId> returningShips = new HashSet<EntityId>();
    //map-ul retine tintele alese de fiecare nava
    static Map<EntityId, Position> targets = new HashMap<EntityId, Position>();

    public static boolean isReturning(Ship ship) {
        return returningShips.contains(ship.id);
    }

    public static boolean hasTarget(Ship ship) {
        return targets.containsKey(ship.id);
    }

    //returneaza o matrice de n*n poztii normalizate in jurul pozitiei shipPos
    public static Position[][] getPositions(GameMap gameMap, Position shipPos, int n) {
        Position[][] positions = new Position[n][n];
        for (int x = shipPos.x - n / 2; x <= shipPos.x + n / 2; x++) {
            for (int y = shipPos.y - n / 2; y <= shipPos.y + n / 2; y++) {
                positions[x - shipPos.x + n / 2][y - shipPos.y + n / 2] = gameMap.normalize(new Position(x, y));
            }
        }
        return positions;
    }

    public static int getAverageScore(GameMap gameMap, Position[][] pos, int n){
        int sum = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                sum += gameMap.at(pos[i][j]).halite;
        }
        sum = sum / (n*n);
        return sum;
    }

    //functia decide miscarile pentru navele care nu se intorc la baza
    public static void setOtherShips(ArrayList<Command> commandQueue, GameMap gameMap, Player me, int medie) {
        for (final Ship ship : me.ships.values()) {


            if (!isReturning(ship)) {

                //daca nava tocmai a iesit din shipyard nu poate sa mai ia si o alta comanda
                if (ship.isExiting == true) {
                    ship.isExiting = false;
                    continue;
                }

                //daca nava nu are resursele necesare pentru a continua drumul atunci se opreste
                if (ship.halite < gameMap.at(ship).halite / 10) {
                    commandQueue.add(ship.stayStill());
                    continue;
                }

                //daca nava ajunge pe o casuta cu suficient halite atunci ramane pe loc
                if (gameMap.at(ship).halite > 3 * medie / 7) {
                    commandQueue.add(ship.stayStill());
                    continue;
                }


                //daca nava are deja un target setat verificam daca mai are rost sa isi continue drumul spre target
                if (hasTarget(ship)) {

                    Position[][] pos = getPositions(gameMap, targets.get(ship.id), 3);
                    int sum = getAverageScore(gameMap, pos, 3);

                    if (sum >  4*medie/5  && gameMap.calculateDistance(ship.position, targets.get(ship.id)) > 2) {
                        gameMap.at(targets.get(ship.id)).isTargeted = true;
                        Direction dir = gameMap.naiveNavigate(ship, targets.get(ship.id));
                        if(dir != Direction.STILL){
                            commandQueue.add(ship.move(dir));
                            continue;
                        }
                        targets.remove(ship.id);

                    } else {
                        targets.remove(ship.id);
                    }
                }

                //marim raza de cautare pana cand gasim o celula satisfacatoare
                //gasirea celulei se face random, prin 3*n incercari din care se alege maximul
                Position[][] positions;
                Random rand = new Random();
                int radius = 3;
                do {
                    positions = getPositions(gameMap, ship.position, radius);
                    int randomX, randomY;
                    int tries = 0;
                    int max = -1;
                    Position position = null;
                    while (tries < 3 * radius) {
                        randomX = rand.nextInt(radius);
                        randomY = rand.nextInt(radius);
                        Position[][] pos = getPositions(gameMap, positions[randomX][randomY], 3);
                        int sum = getAverageScore(gameMap, pos, 3);
                        if (sum > max && gameMap.at(positions[randomX][randomY]).isTargeted == false) {
                            max = sum;
                            position = positions[randomX][randomY];
                        }
                        tries++;
                    }
                    //daca celula gasita respecta conditia atunci o alegem ca tinta
                    if (max > 3 * medie / 5) {
                        Direction dir = gameMap.naiveNavigate(ship, position);
                        if (dir != Direction.STILL) {
                            commandQueue.add(ship.move(dir));
                            targets.put(ship.id, position);
                            gameMap.at(position).isTargeted = true;
                            break;
                        }

                    }
                    radius += 2;
                } while (radius < 4 * gameMap.width / 5);
                if (radius >= 4 * gameMap.width / 5)
                    commandQueue.add(ship.stayStill());

            }
        }
    }

    //functia decide miscarile pentru navele care se intorc la baza
    public static void setReturningShips(ArrayList<Command> commandQueue, Game game, GameMap gameMap, Player me, int medie) {
        for (final Ship ship : me.ships.values()) {
            if (isReturning(ship)) {
                //daca nava ramane fara resurse atunci sta pe loc
                if (ship.halite < gameMap.at(ship).halite / 10) {
                    commandQueue.add(ship.stayStill());
                    continue;
                }

                //vom trata acest caz separat
                if (ship.position.equals(me.shipyard.position)) {
                    continue;
                }

                //daca o nava se afla la distanta 1 de shipyard si in shipyard se afla o nava blocata din toate partile
                //atunci aceste 2 nave fac switch intre ele
                if (gameMap.calculateDistance(ship.position, me.shipyard.position) == 1) {
                    boolean found = false;
		    
                    for (final Ship stuckShip : me.ships.values()) {
                        if (stuckShip.isStuck) {
                            gameMap.at(ship.position).ship = null;
                            gameMap.at(stuckShip.position).ship = null;
                            stuckShip.isStuck = false;
                            stuckShip.isExiting = true;
                            returningShips.remove(stuckShip.id);
                            commandQueue.add(ship.move(gameMap.naiveNavigate(ship, stuckShip.position)));
                            commandQueue.add(stuckShip.move(gameMap.naiveNavigate(stuckShip, ship.position)));
                            gameMap.at(ship.position).ship = stuckShip;
                            gameMap.at(stuckShip.position).ship = ship;
                            found = true;
                            break;
                        }
                    }
                    if (found) continue;
                }

                //daca se merita sa ne oprim pe drumul de intoarcere atunci o facem
                if(gameMap.at(ship).halite > 250 && ship.halite < 890){
                    commandQueue.add(ship.stayStill());
                    continue;
                }
                //altfel inaintam spre shipyard
                commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position)));
            }

            //daca o nava s-a umplut atunci o trimitem spre baza
            if (!isReturning(ship) && ship.halite >= Constants.MAX_HALITE * 9.5 / 10) {
                returningShips.add(ship.id);
                targets.remove(ship.id);
                commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position)));
                continue;
            }


        }
    }
    //calculeaza tot halite-le de pe harta
    public static int getTotalHalite(GameMap gameMap) {
        int total = 0;
        for (int i = 0; i < gameMap.height; i++)
            for (MapCell cell : gameMap.cells[i]) {
                total += cell.halite;
            }
        return total;
    }

    //functia care determina miscarile pentru toate testoasele
    public static void getMoves(ArrayList<Command> commandQueue, Game game, GameMap gameMap, Player me) {

        //cauculam media de halite pe toata harta
        int totalHalite = getTotalHalite(gameMap);
        int medieHalite = totalHalite / (gameMap.width * gameMap.height);

        //tratam separat navele care sunt pe shipyard cautandu-le un loc random de iesire
        //daca sunt blocate atunci setam isStuck pe true;
if(gameMap.at(me.shipyard.position).ship != null && !gameMap.at(me.shipyard.position).ship.owner.equals(me.id)) gameMap.at(me.shipyard.position).ship = null;
        for (final Ship ship : me.ships.values()) {
            if (ship.position.equals(me.shipyard.position)) {

                returningShips.add(ship.id);
                ArrayList<Position> positions = ship.position.getSurroundingCardinals();
                boolean found = false;
                for (Position pos : positions) {
                    if (!gameMap.at(pos).isOccupied()) {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, pos)));
                        returningShips.remove(ship.id);
                        ship.isExiting = true;
                        gameMap.at(ship.position).ship = null;
                        found = true;
                        break;
                    }
                }
                if (found == false) {
                    ship.isStuck = true;
                }
            }
        }

        setReturningShips(commandQueue, game, gameMap, me, medieHalite);

        setOtherShips(commandQueue, gameMap, me, medieHalite);

        for (int i = 0; i < gameMap.height; i++) {
            for (MapCell cell : gameMap.cells[i]) {
                cell.isTargeted = false;
            }
        }

        //daca s-a eliberat vreun loc pe langa shipyard mutam nava blocata
        for (final Ship ship : me.ships.values()) {
            if (ship.isStuck) {
                ArrayList<Position> positions = ship.position.getSurroundingCardinals();
                for (Position pos : positions) {
                    if (!gameMap.at(pos).isOccupied()) {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, pos)));
                        returningShips.remove(ship.id);
                        break;
                    }
                }
            }
        }

        //spawnam nave daca runda e sub 200 si nu se afla in jurul shipyard-ului mai mult de o nava care se intoarce
        int count = 0;
        ArrayList<Position> shipyardPositions = me.shipyard.position.getSurroundingCardinals();
        for (Position pos : shipyardPositions) {
            if (gameMap.at(pos).isOccupied()) {
                if (isReturning(gameMap.at(pos).ship)) count++;
            }
        }
        if (
                game.turnNumber <= 200 &&
                        me.halite >= Constants.SHIP_COST &&
                        !gameMap.at(me.shipyard).isOccupied() && count < 2) {
            commandQueue.add(me.shipyard.spawn());

        }


    }

    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        for (; ; ) {

            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            ArrayList<Command> commandQueue = new ArrayList<Command>();
            getMoves(commandQueue, game, gameMap, me);
            game.endTurn(commandQueue);
        }
    }
}
