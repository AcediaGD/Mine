 /*     ████
     ████████				████████████████████████████████████████████████████████████████
   ▒▒██████████▒▒			█                                                              █
   ▒▒   ███████▒▒			█    ██████  ██████  ██   ██     █     ███    ██    ██████	   █
   ██      ██████▒▒		    █   ██      ██    ██ ██   ██    █ █    ████   ██    █     █    █
    ███    ██████▒▒		    █   ██   ██ ██    ██ ███████   █████   ██ ██  ██    ██████     █
           ████████▒▒		█   ██    █ ██    ██ ██   ██  ██   ██  ██  ██ ██    █     █    █
         ▒▒████████▒▒██     █    ██████  ██████  ██   ██ ██     ██ ██   ████    ██████  █  █
       ▒▒▒▒████████▒▒▒▒██   █															   █
         ▒▒████████▒▒██     █			    ██		█	   ██		██		█			   █
           ████████         █			    ██	   █ █		██	   ██	   █ █			   █
    ███    ██████▒▒         █			    ██	  █████		 ██	  ██	  █████		       █
   ██      ██████▒▒         █		    ██	██	 ██	  ██	  ██ ██ 	 ██	  ██ 	       █
   ▒▒    ██████▒▒           █	   ██	  ██	██	   ██		█		██	   ██		   █
   ▒▒██████████▒▒           █															   █
     ████████               ████████████████████████████████████████████████████████████████
   	    ███	   */

// Youtube Link: https://youtu.be/s-Cyot-qFX8

// btw this is the skeleton code for the GUI to run the game 
// so don't be confused if you don't see a main method

import java.util.Random;

public class Grid {

private boolean[][] bombGrid;
private int[][] countGrid;
private int numRows;
private int numColumns;
private int numBombs;

// 10x10 grid, and the 25 bombs
public Grid() {
    this.numRows = 10;
    this.numColumns = 10;
    this.numBombs = 25;

    createBombGrid();
    createCountGrid();
}

// Extra-credit constructor which is the rows x columns, 25 bombs
public Grid(int rows, int columns) {
    this.numRows = rows;
    this.numColumns = columns;
    this.numBombs = 25;

    createBombGrid();
    createCountGrid();
}

// Extra-credit constructor which is the rows x columns, numBombs bombs
public Grid(int rows, int columns, int numBombs) {
    this.numRows = rows;
    this.numColumns = columns;
    this.numBombs = numBombs;

    createBombGrid();
    createCountGrid();
}

// getters
public int getNumRows() {
    return numRows;
}

public int getNumColumns() {
    return numColumns;
}

public int getNumBombs() {
    return numBombs;
}

// Return deep copy of bombGrid
public boolean[][] getBombGrid() {
    boolean[][] copy = new boolean[numRows][numColumns];
    for (int r = 0; r < numRows; r++) {
        System.arraycopy(bombGrid[r], 0, copy[r], 0, numColumns);
    }
    return copy;
}

// Return deep copy of countGrid
public int[][] getCountGrid() {
    int[][] copy = new int[numRows][numColumns];
    for (int r = 0; r < numRows; r++) {
        System.arraycopy(countGrid[r], 0, copy[r], 0, numColumns);
    }
    return copy;
}

// Is there a bomb at (row, column)? who knows maybe you do or maybe not!
public boolean isBombAtLocation(int row, int column) {
    return bombGrid[row][column];
}

// Count value at (row, column) from countGrid
public int getCountAtLocation(int row, int column) {
    return countGrid[row][column];
}

/*=================================================================

	█████	█  ████  █  █		 █  ▄███▄    ███████ ███████
	█	 █  ███          █	    █  █     █      █    █
	█████	█        █    █	   █   █     █      █    ███████
	█		█        █     █  █    ███████      █    █
	█		█        █      ██     █     █      █    ███████

         HELPERS TO BUILD THE GRIDS & extra creds 🗿
=================================================================*/

// Allocate and randomly place bombs for the extra credit LOL
private void createBombGrid() {
    bombGrid = new boolean[numRows][numColumns];

    Random random = new Random();
    int placed = 0;

    while (placed < numBombs) {
        int r = random.nextInt(numRows);
        int c = random.nextInt(numColumns);

        // place only if empty, this goes back to the random java.util
        if (!bombGrid[r][c]) {
            bombGrid[r][c] = true;
            placed++;
        }
    }
}

// Build countGrid based on bombGrid
private void createCountGrid() {
    countGrid = new int[numRows][numColumns];

  for (int r = 0; r < numRows; r++) {
    for (int c = 0; c < numColumns; c++) {
        int count = 0;

        // Check this cell and all 8 cells around it for bombs
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = r + dr;
                int nc = c + dc;

                if (nr >= 0 && nr < numRows &&
                    nc >= 0 && nc < numColumns &&
                    bombGrid[nr][nc]) {
                    count++;
                }
            }
        }
            countGrid[r][c] = count;
        }
    }
 }
}