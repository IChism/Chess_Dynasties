package ceov2.org;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
//this class deals with all the logic of a live game.
public class GameState {
    //holds morale of both players
    int[] moraleTotals=new int[2];
    //playerturn tells who's turn it is, 1=player 1's turn, 2=player 2's turn
     int playerTurn=1;
     //tells if a piece is selected by a player(they have clicked it)
     boolean pieceSelected;
     //the location of the selected piece
     int selectedPieceLocx;
     int selectedPieceLocy;
     //the index of the selected piece in the array of pieces
     int selectedPiece;
     //booleans for the state of the game
     boolean whiteWins=false;
     boolean blackWins=false;
     boolean gameOver=false;

     //font object for writing morale values to the screen
     BitmapFont font;

     //boardPieces is only used because Piece files do not exist yet
    ArrayList<Piece> tempPieces = new ArrayList<Piece>();
    //the array of all Pieces that are on the board
    ArrayList<Piece> allPiecesOnBoard=new ArrayList<Piece>();
    //contains the information about what occupies each square of the board
    //0=empty square, 1=square occupied by white piece, 2=square occupied by black piece
    int[][] boardState = new int[8][8];
    //contains the information about what specific piece is on each square of the board
    //i.e. it is equal to the index of the piece in the array of Pieces,-1 means unoccupied
    int[][] piecesOnBoard = new int[8][8];

    //in the future the constructor will take 2 army objects as it's parameters to set up the game
    public GameState() {

        initFont();
        Piece.InitAllMoveTypes();
        loadTempPieces();
        loadArmies();

        setBoard(allPiecesOnBoard);
        findAllValidMoves();
    }
    //this method executes every tick
    public void runGame(SpriteBatch batch,MouseVars mouseVars){
        //see if the player has clicked on a piece, or it trying to move a piece
        //if the player has made a valid move this method will also execute it
        processMouseInput(mouseVars);
        //draw everything
        batch.begin();
        drawAll(batch);
        batch.end();
    }

    private void initFont(){
    font= new BitmapFont();
    font.setColor(Color.BLACK);
}
//put all pieces on the board, set their locations
    public void setBoard(ArrayList <Piece> pieces) {
        allPiecesOnBoard=pieces;
        //set pieceOnBoard to -1, which means the square is unnoccupied
        for(int x=0;x!=8;x++){
            for(int y=0;y!=8;y++){
                piecesOnBoard[x][y]=-1;
            }
        }
        int counter=0;
        //which row the pieces will be placed on, from 0-7
        int row=0;
        //what colour the pieces are, 1 means white, 2 means black
        int colour=0;

        //4*8 loop to loop through all 32 pieces
        for (int x = 0; x != 4; x++) {
            //the row and colour are set the the right numbers
            if (x==0){
                row=1;
                colour=1;
            }
            if (x==1){
                row=0;
            }
            if (x==2){
                colour=2;
                row=6;
            }
            if (x==3){
                row=7;
            }
            //the boards values are updated
            for (int y = 0; y != 8; y++) {
                //the array boardState is set to the colour of the piece, a value of 0 means unoccupied
                boardState[y][row] = colour;
                if (colour==1){
                    moraleTotals[0]+=allPiecesOnBoard.get(counter).moraleCost;
                }else{
                    if(colour==2) {
                        moraleTotals[1] += allPiecesOnBoard.get(counter).moraleCost;
                    }
                }
                //The array of pieces: each piece has it's position on the board set
                allPiecesOnBoard.get(counter).setLocation(y, row);
                //the array piecesOnBoard is set to the index of the piece
                piecesOnBoard[y][row]=counter;
                counter++;
            }
        }
    }

//find all the valid moves on any given turn
    public void findAllValidMoves(){
//loop through the arrayList of pieces
for(int a=0;a!=allPiecesOnBoard.size();a++){
//loop through the moveset of each piece
    for(int x=0;x!=15;x++){
        for(int y=0;y!=15;y++) {
            //if the piece has been captured, obviously it can't move
            if(allPiecesOnBoard.get(a).captured==false) {
                //if the piece has no movement on the square, obviously it can't move there
                if (allPiecesOnBoard.get(a).moveset[x][y] != 0) {
                    //using the location of the move in the 15*15 array moveset, and the location of the piece being moved
                    //the destination of the move on the board is found
                    int xOffset = x - 7;
                    int yOffset = y - 7;
                    int xOnBoard = allPiecesOnBoard.get(a).xLocation + xOffset;
                    int yOnBoard = allPiecesOnBoard.get(a).yLocation + yOffset;
                    //if the destination is off the board,obviously the piece can't move there
                    if (xOnBoard >= 0 && xOnBoard <= 7 && yOnBoard >= 0 && yOnBoard <= 7) {
                        //test if the square is a valid target for the piece to move to
                        //for example a piece that can move/attack can go to an empty square or an enemy occupied square
                        //but not an ally square
                        boolean validTarget = Piece.allMoveTypes.get(allPiecesOnBoard.get(a).moveset[x][y]).checkIsValidTarget(boardState[xOnBoard][yOnBoard],playerTurn);
                        //if the target is not valid, the piece can't move there
                        if (validTarget == true) {
                            //check if the piece is blocked, (bishops can't move through other pieces)
                            boolean blocked=false;
                            //some movetypes can't be blocked, so if that isn't an issue, blocked remains false
                            if (Piece.allMoveTypes.get(allPiecesOnBoard.get(a).moveset[x][y]).blockable==true){
                                //method to check if the piece is blocked
blocked=checkIfPieceIsBlocked(xOnBoard,yOnBoard,allPiecesOnBoard.get(a).xLocation,allPiecesOnBoard.get(a).yLocation);
                            }

                              //if the move is not blocked, the piece object's array of valid moves
                              //has that index marked true, meaning it can make that move
                            if (blocked==false) {
                                allPiecesOnBoard.get(a).validMoves[x][y] = true;
                            }
                        }
                    }
                }
            }
        }
    }
}
}



//use mouse input to see if the player is trying to make a move
    public void processMouseInput(MouseVars mouseVars){
        //loc is set to the squares on the board that the mouse cursor is located at
        int[] loc = findSquareMouseIsOn(mouseVars.mousePosx, mouseVars.mousePosy);

        if (mouseVars.mouseClicked==true) {
            //if loc is actually on the board, we have to test to see if the square is occupied
            if (loc[0] >= 0 && loc[1] >= 0&&loc[0]<=7&&loc[1]<=7) {
                //if the square is a piece the player owns, they might be able to pick it up
                if (boardState[loc[0]][loc[1]] == playerTurn) {
                    //if they haven't already selected a piece, the player can pick up a new piece
                    if (pieceSelected==false) {
                        //the piece is selected
                        allPiecesOnBoard.get(piecesOnBoard[loc[0]][loc[1]]).select();
                        //the selected pieces location and index are stored
                        selectedPieceLocx=loc[0];
                        selectedPieceLocy=loc[1];
                        selectedPiece=piecesOnBoard[loc[0]][loc[1]];
                        //pieceSelected is set true, so 2 pieces can't be selected at once
                        pieceSelected = true;
                    }
                }
            }
        }


        if (mouseVars.mouseReleased==true){

            boolean validMove=false;
            //find the index for the moveset array to use
            int xOffset=7+loc[0]-allPiecesOnBoard.get(selectedPiece).xLocation;
            int yOffset=7+loc[1]-allPiecesOnBoard.get(selectedPiece).yLocation;
            //if a piece is selected, we know the player has attempted to make a move
            if (pieceSelected==true) {
                //if the mouse is outside the board, then no move can be made
                if (loc[0] >= 0 && loc[1] >= 0 && loc[0] <= 7 && loc[1] <= 7) {
                    //check the validMoves array to see if the square targeted is valid
                    if (allPiecesOnBoard.get(selectedPiece).validMoves[xOffset][yOffset] == true) {
                        validMove = true;
                    }
                }
            }

            //if the move was found to be valid
            if (validMove==true){
                //execute the move
                allPiecesOnBoard.get(selectedPiece).executeMove(loc[0],loc[1],this);
                //set up for the next turn
                updateBoard();

            }else{
                //if the move was invalid, the piece is unselected
                allPiecesOnBoard.get(piecesOnBoard[selectedPieceLocx][selectedPieceLocy]).unselect();
            }
            // this happens any time the mouse is released
            pieceSelected=false;
        }
    }
    //this method is currently unused
    void loadPieces(){
        int[][] moveset=new int[15][15];
        String line="2,3,1,1,1,1,1,1,1,1,5";
        String line2="1,2,1,1,1,1,1,1,1,1,4";
        String line3="4,1,0,1,1,0,1,1,1,1";
        // line="10,9,8,7,6,5,4,3,2,1";
//setMoveSet(moveset,line);
        // setMoveSet(moveset,line2);
        // setMoveSet(moveset,line3);
    }
    //loads the default pieces, this will no longer be used once pieces are loaded from files
    private void loadArmies(){
        //load all white pieces first, so they take up the first 16 places in the array
        for (int x=0;x!=8;x++) {
            loadPieceToArmy("Pawn",true);

        }
        loadPieceToArmy("Rook",true);
        loadPieceToArmy("Knight",true);
        loadPieceToArmy("Bishop",true);
        loadPieceToArmy("Queen",true);
        loadPieceToArmy("King",true);
        loadPieceToArmy("Bishop",true);
        loadPieceToArmy("Knight",true);
        loadPieceToArmy("Rook",true);
        for(int x=0;x!=8;x++){
            loadPieceToArmy("Pawn",false);
        }


        loadPieceToArmy("Rook",false);
        loadPieceToArmy("Knight",false);
        loadPieceToArmy("Bishop",false);
        loadPieceToArmy("Queen",false);
        loadPieceToArmy("King",false);
        loadPieceToArmy("Bishop",false);
        loadPieceToArmy("Knight",false);
        loadPieceToArmy("Rook",false);
        tempPieces.clear();
    }
    //add a piece to the arraylist containing all the pieces
    private void loadPieceToArmy(String pieceName,boolean isWhite){
        //loop through boardPieces to find the piece with the correct name
        for (int x=0;x!=tempPieces.size();x++){
            //if the name is correct
            if (tempPieces.get(x).name.equalsIgnoreCase(pieceName)){
                //initialize the piece object and add it to the ArrayList
                allPiecesOnBoard.add(new Piece(tempPieces.get(x).moveset,pieceName));
                //set the image and color of the piece
                allPiecesOnBoard.get(allPiecesOnBoard.size()-1).setImage(isWhite);
                //set the morale values of the piece
                allPiecesOnBoard.get(allPiecesOnBoard.size()-1).setMoraleValues(tempPieces.get(x).moraleCost,tempPieces.get(x).moralePenalty);
                //add the morale values to the totals for each player
                break;
            }
        }
    }
    //is only used because Piece files do not exist yet
    private void loadTempPieces() {
        int[][] moveset = new int[15][15];
        //index 0
        setMoveSetLines(moveset,7,true,false,true,false,true,false,true,false,1);
        tempPieces.add(new Piece(moveset,"Rook"));
        resetMoveset(moveset);
        tempPieces.get(0).setMoraleValues(50,0);
        //index 1
        setMoveSetLines(moveset,7,true,true,true,true,true,true,true,true,1);
        tempPieces.add(new Piece(moveset,"Queen"));
        resetMoveset(moveset);
        tempPieces.get(1).setMoraleValues(90,0);
        //index 2
        setMoveSetLines(moveset,1,true,true,true,true,true,true,true,true,1);
        tempPieces.add(new Piece(moveset,"King"));
        resetMoveset(moveset);
        tempPieces.get(2).setMoraleValues(0,85);
        //index 3
        setMoveSetLines(moveset,7,false,true,false,true,false,true,false,true,1);
        tempPieces.add(new Piece(moveset,"Bishop"));
        resetMoveset(moveset);
        tempPieces.get(3).setMoraleValues(30,0);
        //index 4
        setMoveSetCoord(moveset,1,2,true,true,true,true,true,true,true,true,4);
        tempPieces.add(new Piece(moveset,"Knight"));
        resetMoveset(moveset);
        tempPieces.get(4).setMoraleValues(30,0);
        //index 5
        setMoveSetLines(moveset,2,true,false,false,false,false,false,false,false,5);
        setMoveSetLines(moveset,1,true,false,false,false,false,false,false,false,2);
        setMoveSetLines(moveset,1,false,true,false,false,false,false,false,true,3);

        tempPieces.add(new Piece(moveset,"Pawn"));
        resetMoveset(moveset);
        tempPieces.get(5).setMoraleValues(10,0);

    }
    //if a string has is "1", return true, otherwise return false
    boolean convertToBoolean(String convert){
        boolean converted=false;
        if (Integer.valueOf(convert)==1){
            converted=true;
        }
        return converted;
    }
    //set the moveset array values, given a String
    void setMoveSet(int[][] moveset,String line){
        //9 commas means that we are dealing with linear movements,10 variables, 1 range, 8 direction, 1 movetype
        //10 commas means we are dealing with knight-like movements,11 variables, 2 coordinates, 8 directions, 1 movetype
        int commaCounter=0;
        for(int x=0;x!=line.length();x++){
            if (line.charAt(x)==','){
                commaCounter++;
            }
        }

        //split the line string into it's 10 or 11 smaller strings
        String[] separated=new String[11];
        for(int x=0;x!=commaCounter;x++) {
            separated = line.split(",");
        }

        if (commaCounter==9){
            setMoveSetLines(moveset,Integer.valueOf(separated[0]),convertToBoolean(separated[1]),convertToBoolean(separated[2]),convertToBoolean(separated[3]),convertToBoolean(separated[4]),convertToBoolean(separated[5]),convertToBoolean(separated[6]),convertToBoolean(separated[7]),convertToBoolean(separated[8]),Integer.valueOf(separated[9]));
        }
        if (commaCounter==10){
            setMoveSetCoord(moveset,Integer.valueOf(separated[0]),Integer.valueOf(separated[1]),convertToBoolean(separated[2]),convertToBoolean(separated[3]),convertToBoolean(separated[4]),convertToBoolean(separated[5]),convertToBoolean(separated[6]),convertToBoolean(separated[7]),convertToBoolean(separated[8]),convertToBoolean(separated[9]),Integer.valueOf(separated[10]));
        }
    }
    //set the moveset array to all 0's
    private void resetMoveset(int[][] moveset){
        String line="";
        for (int y=14;y!=-1;y--){
            for(int x=0;x!=15;x++){
                line+=String.valueOf(moveset[x][y])+" ";
                moveset[x][y]=0;
            }
            System.out.println(line);
            line="";
        }

    }
    /*
 update the "moveset" array for a piece, by adding a linear movement
 the integer movetype indicates the type of movement the piece gets, 1 is move/attack for example.
 depending on the values of the booleans, different parts of the array moveset will be changed
 for example, a rook with north=true but all other booleans false would only be able to move forwards
 */
    private void setMoveSetLines(int[][] moveset,int range, boolean north, boolean northeast,boolean east, boolean southeast, boolean south,boolean southwest, boolean west, boolean northwest,int movetype){
        if (south == true) {
            for (int y = 0; y != 7; y++) {
                if (Math.abs(7 - y) <= range) {
                    moveset[7][y] = movetype;
                }
            }
        }
        if (north == true) {
            for (int y = 8; y != 15; y++) {
                if (Math.abs(7 - y) <= range) {
                    moveset[7][y] = movetype;
                }
            }
        }
        if (west == true) {
            for (int x = 8; x != 15; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[x][7] = movetype;
                }
            }
        }
        if (east == true) {
            for (int x = 0; x != 7; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[x][7] = movetype;
                }
            }
        }

        if (northeast==true){
            for (int x = 8; x != 15; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[x][x] = movetype;
                }
            }
        }
        if (southwest==true){
            for (int x = 8; x != 15; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[14-x][14-x] = movetype;
                }
            }
        }
        if (northwest==true){
            for (int x = 8; x != 15; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[14-x][x] = movetype;
                }
            }
        }
        if (southeast==true){
            for (int x = 8; x != 15; x++) {
                if (Math.abs(7 - x) <= range) {
                    moveset[x][14-x] = movetype;
                }
            }
        }


    }
    /*
    update the "moveset" array for a piece, by adding a knight-like movement
    coord1 and coord2 tell the method which parts of array moveset to change
    for example, for knight the coord variables would be 1 and 2. because a knight moves up 2 and over 1
    the integer movetype indicates the type of movement the piece gets, 1 is move/attack for example.
    depending on the values of the booleans, different parts of the array moveset will be changed
    for example, a knight with upRight true but all other booleans false would only be able to move
    to a square 2 up and 1 right of itself.
    */
    private void setMoveSetCoord(int[][] moveset,int coord1, int coord2, boolean upRight, boolean rightUp,boolean rightDown, boolean downRight, boolean downLeft,boolean leftDown, boolean leftUp, boolean upLeft,int movetype) {
        int biggerNumber;
        int smallerNumber;
        //make sure the bigger and smaller numbers are always in the same variables.
        //this ensures that if coord1 and coord2's values are swapped they still produce the same moveset
        //i.e 1,2 is the same as 2,1
        if (coord1>coord2){
            biggerNumber=coord1;
            smallerNumber=coord2;
        }else{
            biggerNumber=coord2;
            smallerNumber=coord1;
        }

        if (upRight==true){
            moveset[7+smallerNumber][7+biggerNumber]=movetype;
        }
        if (rightUp==true){
            moveset[7+biggerNumber][7+smallerNumber]=movetype;
        }

        if (rightDown==true){
            moveset[7+biggerNumber][7-smallerNumber]=movetype;
        }
        if (downRight==true){
            moveset[7+smallerNumber][7-biggerNumber]=movetype;
        }

        if (downLeft==true){
            moveset[7-smallerNumber][7-biggerNumber]=movetype;
        }
        if (leftDown==true){
            moveset[7-biggerNumber][7-smallerNumber]=movetype;
        }

        if (leftUp==true){
            moveset[7-biggerNumber][7+smallerNumber]=movetype;
        }
        if (upLeft==true){
            moveset[7-smallerNumber][7+biggerNumber]=movetype;
        }

    }

    //prepare the board for the next turn
    private void updateBoard(){
        //change who's turn it is
        if (playerTurn==1) {
            playerTurn = 2;
        }else{
            playerTurn=1;
        }
        //reset the valid moves arrays and find the new set of valid moves
            setAllMovesInvalid();
            findAllValidMoves();
        //set all piece selected variables back to default, unselected
        allPiecesOnBoard.get(selectedPiece).unselect();
        pieceSelected=false;
        selectedPieceLocx=0;
        selectedPieceLocy=0;
        selectedPiece=0;
        //see if each player's king is still alive, if not the player suffers a morale penalty on the start
        //of their turn
        checkIfKingLives();
        ///check if any morale values have reached 0, if so end the game
        checkIfGameOver();

    }
//check if a move is blocked by another piece
    private boolean checkIfPieceIsBlocked(int moveTargetx,int moveTargety,int pieceLocx,int pieceLocy){
        boolean blocked=false;
        //find the difference between the target and the pieces location
        //for example, a piece on square 0,0 trying to move to 3,3 has xDiff and yDiff =3
        int xDiff=moveTargetx-pieceLocx;
        int yDiff=moveTargety-pieceLocy;

//if neither xDiff or yDiff are equal to zero
       // if (xDiff!=0&&yDiff!=0){
            //values decreased by 1, if a piece has a xDiff of 3, that means there are only 2 squares
            //that could potentially block the piece from moving
            xDiff=decreaseAbsValueByOne(xDiff);
            yDiff=decreaseAbsValueByOne(yDiff);
            //loop through "xDiff" times until xDiff=0
            //every loop decreases xDiff and yDiff by 1, unless they are 0, in which case they stay the same
        if (xDiff!=0) {
            for (int x = xDiff; x != 0; x = decreaseAbsValueByOne(x), yDiff = decreaseAbsValueByOne(yDiff)) {
                //if the location on the board is occupied, blocked is set true
                if (boardState[pieceLocx + x][pieceLocy + yDiff] != 0) {
                    blocked = true;
                    break;
                }
            }
        }else{
            //or if, xDiff=0, loop through yDiff times
            for (int y = yDiff; y != 0; y = decreaseAbsValueByOne(y), xDiff = decreaseAbsValueByOne(xDiff)) {
                //if the location on the board is occupied, blocked is set true
                if (boardState[pieceLocx + xDiff][pieceLocy + y] != 0) {
                    blocked = true;
                    break;
                }
            }
        }

        return blocked;
    }
//decrease the absolute value of any integer by 1, if it is 0, it stays at 0
    private int decreaseAbsValueByOne(int x){
        if (x>0){
            x--;
        }
        if (x<0){
            x++;
        }
        return x;
    }
//find which square on the board the cursor is on
    private int[] findSquareMouseIsOn(int mousex,int mousey){
        //reset these values, these will be the values of the mouse's location on screen
        int xLoc=-1;
        int yLoc=-1;
        //if the cursor is within the bounds of the board
        if (mousex>Piece.boardLocationx&&mousex<Piece.boardLocationx+480) {
            for (int x = 0; x != 8; x++) {
                //if the mouse position on screen is greater than the square's leftmost point
                if (mousex > Piece.boardLocationx + 60 * x) {
                    xLoc++;
                } else {
                    break;
                }
            }
        }
        //if the cursor is within the bounds of the board
    if (mousey>Piece.boardLocationy&&mousey<Piece.boardLocationy+480) {
        for (int x = 0; x != 8; x++) {
            //if the mouse position on screen is greater than the square's bottommost point
            if (mousey > Piece.boardLocationy + 60 * x) {
                yLoc++;
            } else {
                break;
            }
        }

    }
    //set the loc array to the locations found
    int[] loc=new int[2];
        loc[0]=xLoc;
        loc[1]=yLoc;
    return loc;
}
    //loop through all pieces and set all their possible moves as invalid
    private void setAllMovesInvalid(){
        for(int a=0;a!=allPiecesOnBoard.size();a++){
            for(int x=0;x!=15;x++){
                for(int y=0;y!=15;y++){
                    allPiecesOnBoard.get(a).validMoves[x][y]=false;
                }
            }
        }
}
//loop through the pieces array and see if the piece named "king" has been captured
    private void checkIfKingLives(){
        for(int x=0;x!=allPiecesOnBoard.size();x++){
            //if the piece is named "king"
            if(allPiecesOnBoard.get(x).name.equalsIgnoreCase("king")){
                //if the king is captured
                if (allPiecesOnBoard.get(x).captured==true){
                    //if the king is of the colour of the player whos turn just started
                    if (playerTurn==1&&allPiecesOnBoard.get(x).isWhite==true){
                        //reduce that players morale total
                        moraleTotals[0]-=15;
                    }
                    if(playerTurn==2&&allPiecesOnBoard.get(x).isWhite==false){
                        moraleTotals[1]-=15;
                    }
                }
            }
        }
}
//if the morale of a player is 0, set the gamestate booleans according to which player hit 0 morale
    private void checkIfGameOver(){
        if (moraleTotals[0]<=0){
            moraleTotals[0]=0;
            blackWins=true;
            gameOver=true;
        }
        if (moraleTotals[1]<=0){
            moraleTotals[1]=0;
            gameOver=true;
            whiteWins=true;
        }

}

    private void drawAll(SpriteBatch batch) {
        String draw="";
        //draw the morale totals on screen
        draw="White's Morale Total: "+String.valueOf(moraleTotals[0]);
        font.draw(batch,draw,600,25);
        draw="Black's Morale Total: "+String.valueOf(moraleTotals[1]);
        font.draw(batch,draw,600,555);
//if the game is over, draw a message saying who won
        if (gameOver==true){
            if (blackWins==true){
                draw="Black Wins!";
            }
            if(whiteWins==true){
                draw="White Wins!";
            }

            font.draw(batch,draw,600,600);
        }

        //loop through all pieces that have not been captured and draw them
        for (int x = 0; x != allPiecesOnBoard.size(); x++) {
            if (allPiecesOnBoard.get(x).captured==false) {
                allPiecesOnBoard.get(x).draw(batch);
            }
        }
    }
    //delete graphics objects, used when the game is being reset to avoid leaking memory
    void deleteGraphics(){
        font.dispose();
        for(int x=0;x!=allPiecesOnBoard.size();x++){
            allPiecesOnBoard.get(x).deleteGraphics();
        }
    }

}