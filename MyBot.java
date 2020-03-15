import hlt.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

class CursiCell
{
    public Position position;
    public int halite;

    public CursiCell(Position position, int halite) 
    {
        this.position = position;
        this.halite = halite;
    }
}

public class MyBot 
{
    public static boolean IsEnemyInBase(Game game)
    {
        MapCell shipYardCell = game.gameMap.at(game.me.shipyard.position); 
        return shipYardCell.isOccupied() && shipYardCell.ship.owner != game.me.id;
    }

    public static void MoveShipsBack(Game game, ArrayList<Ship> myShips)
    {
        for(Ship ship: myShips)
        {
            if(Constants.MAX_TURNS - game.turnNumber < game.gameMap.calculateDistance(ship.position, game.me.shipyard.position) + myShips.size()) 
            {
                ship.isGoingBackFinally = true;
            }
        } 
    }
    
    // public static Position GetNearestDropOffPosition(Game game, Ship ship)
    // {
    //     Position nearestDropOffPosition = game.me.shipyard.position;
    //     int minNearestDropOff = game.gameMap.calculateDistance(ship.position, game.me.shipyard.position);
        
    //     for (Dropoff dropoff : game.me.dropoffs.values()) 
    //     {
    //         if(game.gameMap.calculateDistance(ship.position, dropoff.position) < minNearestDropOff)
    //         {
    //             nearestDropOffPosition = dropoff.position;
    //             minNearestDropOff = game.gameMap.calculateDistance(ship.position, dropoff.position);
    //         }
    //     }
        
    //     return nearestDropOffPosition;
    // }

    // public static Position GetMaxHaliteZoneCenterPosition(Game game, int range)
    // {
    //     Position maxHalitePosition = null;
    //     int maxHaliteSum = -1;

    //     for(int i=0; i<game.gameMap.height - range; i++)
    //     {
    //         for(int j=0; j<game.gameMap.width - range; j++)
    //         {
    //             int currentHaliteSum = 0;

    //             for(int k=i; k<i + range; k++)
    //             {
    //                 for(int l=j; l<j + range; l++)
    //                 {
    //                     currentHaliteSum += game.gameMap.cells[k][l].halite;
    //                 }
    //             }

    //             if(currentHaliteSum > maxHaliteSum)
    //             {
    //                 maxHaliteSum = currentHaliteSum;
    //                 maxHalitePosition = new Position(i + range / 2, j + range / 2);
    //             }
    //         }
    //     }

    //     return maxHalitePosition;
    // }

    // public static Ship GetNearestShip(Game game, Position desiredPosition)
    // {
    //     Ship nearestShip = null;
    //     int minShipDistance = Integer.MAX_VALUE;

    //     for(Ship ship: game.me.ships.values())
    //     {
    //         int currentDistance = game.gameMap.calculateDistance(ship.position, desiredPosition);
    //         if(currentDistance < minShipDistance)
    //         {
    //             minShipDistance = currentDistance;
    //             nearestShip = ship;
    //         }
    //     }

    //     return nearestShip;
    // }

    public static double GetTotalCurrentHalite(Game game)
    {
        double totalHaliteOnMap = 0;

        for(int i = 0; i < game.gameMap.height; i++)
        {    
            for(int j = 0; j < game.gameMap.width; j++)
            {
                totalHaliteOnMap += game.gameMap.cells[i][j].halite;
            }
        }

        return totalHaliteOnMap;
    }

    public static Direction SmartNavigate(Game game, Ship ship, Position destination) 
    {
        for (final Direction direction : game.gameMap.getUnsafeMoves(ship.position, destination)) 
        {
            final Position targetPos = ship.position.directionalOffset(direction);
            if (!game.gameMap.at(targetPos).isOccupied() || IsEnemyInBase(game) || 
                (ship.isGoingBackFinally && game.gameMap.at(targetPos).structure instanceof Shipyard)) 
            {
                game.gameMap.at(ship.position).ship = null;
                game.gameMap.at(targetPos).markUnsafe(ship);
                
                return direction;
            }
        }

        return Direction.STILL;
    }

    public static void main(final String[] args) 
    {
        final long rngSeed;
        if (args.length > 1) rngSeed = Integer.parseInt(args[1]);
        else rngSeed = System.nanoTime(); 
        final Random rng = new Random(rngSeed);

        Game game = new Game();

        int maximumTuresForSpawn = Constants.MAX_TURNS / 2;
        int maximumHaliteWanted = 850;
        // double initialTotalHalite = GetTotalCurrentHalite(game);
        // int maximumShipsWantedBeforeDropOff = 5;
        // int dropOffSearchRange = 12;

        game.ready("AT/CD");
        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        while(true)
        {
            game.updateFrame();

            ArrayList<Command> commandQueue = new ArrayList<>();
            ArrayList<Ship> myShips = new ArrayList<>(game.me.ships.values());
            Collections.sort(myShips, new Comparator<Ship>() 
            {
                public int compare(Ship ship1, Ship ship2) 
                {
                    int ship1BaseDistance = game.gameMap.calculateDistance(ship1.position, game.me.shipyard.position);
                    int ship2BaseDistance = game.gameMap.calculateDistance(ship2.position, game.me.shipyard.position);
                    return ship1BaseDistance - ship2BaseDistance;
                }
            });
            
            // Position maxHaliteCenterPosition = new Position(game.gameMap.height - 2, -1);
            // Ship nearestShip = GetNearestShip(game, maxHaliteCenterPosition);

            // if(nearestShip != null && game.me.dropoffs.size() == 0 && game.me.halite > Constants.DROPOFF_COST)
            // {
            //     if(nearestShip.position.equals(maxHaliteCenterPosition))  commandQueue.add(nearestShip.makeDropoff());
            //     else commandQueue.add(nearestShip.move(game.gameMap.naiveNavigate(nearestShip, maxHaliteCenterPosition)));
            // }

            MoveShipsBack(game, myShips);

            for (Ship ship : myShips)
            {
                // if(game.me.ships.size() == 1 || !ship.id.equals(nearestShip.id))
                // {
                    if(ship.isGoingBackFinally == true) commandQueue.add(ship.move(SmartNavigate(game, ship, game.me.shipyard.position)));
                    else
                    {
                        ArrayList<CursiCell> surroundingCells = new ArrayList<>();
        
                        for(Position p: ship.position.getSurroundingCardinals())
                        {
                            surroundingCells.add(new CursiCell(p, game.gameMap.at(p).halite));
                        }
        
                        for(CursiCell currentSurroundingCell: surroundingCells)
                        {
                            currentSurroundingCell.halite -= game.gameMap.at(ship.position).halite;
                        }
        
                        surroundingCells.add(new CursiCell(ship.position, game.gameMap.at(ship.position).halite));
        
                        Collections.sort(surroundingCells, new Comparator<CursiCell>() 
                        {
                            public int compare(CursiCell mapCell1, CursiCell mapCell2) 
                            {
                                // double retValue1 = (mapCell2.halite - mapCell1.halite) * 0.5;
                                // double retValue2 = (mapCell1.position.x + mapCell1.position.y - mapCell2.position.x - mapCell2.position.y) * 0.5;
                                // return (int)(retValue1 + retValue2);

                                return ((mapCell2.halite - mapCell1.halite) + ((mapCell1.position.x - mapCell2.position.x) + (mapCell1.position.y - mapCell2.position.y))) / 2;           
                            }
                        });
        
                        if(ship.halite >= maximumHaliteWanted)
                        {
                            commandQueue.add(ship.move(SmartNavigate(game, ship, game.me.shipyard.position)));
                        }
                        else if(ship.halite >= game.gameMap.at(ship.position).halite / (maximumHaliteWanted / 100) || ship.isFull())
                        {
                            for(CursiCell currentSurroundingCell: surroundingCells)
                            {
                                if(currentSurroundingCell.position == ship.position)
                                {
                                    commandQueue.add(ship.move(Direction.STILL));
                                    break;
                                }
                                else if (!game.gameMap.at(currentSurroundingCell.position).isOccupied()) 
                                {
                                    commandQueue.add(ship.move(SmartNavigate(game, ship, currentSurroundingCell.position)));
                                    break;
                                }                        
                            }
                        }
                    }
                // }
            }

            if (game.turnNumber <= maximumTuresForSpawn && game.me.halite >= Constants.SHIP_COST && !game.gameMap.at(game.me.shipyard).isOccupied())
            {
                commandQueue.add(game.me.shipyard.spawn());
            }

            game.endTurn(commandQueue);

            // if(game.me.ships.size() < (game.me.dropoffs.size() == 0 ? maximumShipsWantedBeforeDropOff : maximumShipsWanted) && game.turnNumber <= maximumTuresForSpawn && game.me.halite >= Constants.SHIP_COST && !game.gameMap.at(game.me.shipyard).isOccupied())
            // {
            //     commandQueue.add(game.me.shipyard.spawn());
            // }
           
            // game.endTurn(commandQueue);
        }
    }
}