package SweetSagaQuestGame;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.ViewComponent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.*;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;

public class Main extends GameApplication {

    private Entity[][] candies;
    private String[] candyTypes = {"blue", "green", "orange", "purple", "yellow", "red"};
    private int score;
    private Entity selectedCandy = null;
    private static final int GRID_SIZE = 6;
    private static final int CANDY_SIZE = 60;

    // Directions for BFS traversal (up, down, left, right)
    private static final int[] DIRECTIONS_X = {-1, 1, 0, 0};  // Up, Down, Left, Right
    private static final int[] DIRECTIONS_Y = {0, 0, -1, 1};  // Up, Down, Left, Right

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setWidth(640);
        gameSettings.setHeight(480);
        gameSettings.setTitle("Sweet Saga Quest");
        gameSettings.setVersion("1.0");
        gameSettings.setFullScreenAllowed(false);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("score", 0);
    }

    @Override
    protected void initGame() {
        candies = new Entity[GRID_SIZE][GRID_SIZE];
        int x = 50, y = 40;

        // Initialize candy grid with random candies
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                String candyType = candyTypes[FXGL.random(0, candyTypes.length - 1)];

                // Create candy entity and store the candy type as a custom component
                candies[i][j] = FXGL.entityBuilder()
                        .at(x, y)
                        .viewWithBBox(candyType + ".png")
                        .with(new CandyComponent(candyType))  // Attach the custom CandyComponent to the entity
                        .buildAndAttach();

                final Entity candy = candies[i][j];
                candy.getViewComponent().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleCandyClick(candy));

                x += CANDY_SIZE;
                if ((j + 1) % GRID_SIZE == 0) {
                    x = 50;
                    y += CANDY_SIZE + 10;
                }
            }
        }

        // Score UI
        Text textScore = getUIFactoryService().newText("", Color.BLACK, 22);
        textScore.setTranslateX(480);
        textScore.setTranslateY(200);
        textScore.textProperty().bind(getGameWorld().getProperties().intProperty("score").asString());
        getGameScene().addUINode(textScore);

        // Add Reset Button to the UI
        Button resetButton = new Button("Reset");
        resetButton.setTranslateX(480);
        resetButton.setTranslateY(400);
        resetButton.setOnAction(event -> resetBoard());

        getGameScene().addUINode(resetButton);
    }

    private void handleCandyClick(Entity candy) {
        if (selectedCandy == null) {
            selectedCandy = candy;
            candy.setOpacity(0.5);
        } else {
            if (areAdjacent(selectedCandy, candy)) {
                swapCandies(selectedCandy, candy);
                checkMatches();
            }
            selectedCandy.setOpacity(1);
            selectedCandy = null;
        }
    }

    private boolean areAdjacent(Entity candy1, Entity candy2) {
        int x1 = (int) candy1.getX();
        int y1 = (int) candy1.getY();
        int x2 = (int) candy2.getX();
        int y2 = (int) candy2.getY();

        return (Math.abs(x1 - x2) == CANDY_SIZE && y1 == y2) || (Math.abs(y1 - y2) == CANDY_SIZE + 10 && x1 == x2);
    }

    private void swapCandies(Entity candy1, Entity candy2) {
        // Swap positions
        double tempX = candy1.getX();
        double tempY = candy1.getY();

        candy1.setPosition(candy2.getX(), candy2.getY());
        candy2.setPosition(tempX, tempY);
    }

    private void checkMatches() {
        Set<Entity> matchedCandies = new HashSet<>();

        // Check for horizontal and vertical matches
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Entity candy = candies[row][col];
                if (!matchedCandies.contains(candy)) {
                    Set<Entity> horizontalMatch = new HashSet<>();
                    Set<Entity> verticalMatch = new HashSet<>();
                    bfsMatch(row, col, candy, horizontalMatch, verticalMatch);

                    if (horizontalMatch.size() >= 3) {
                        matchedCandies.addAll(horizontalMatch);
                    }
                    if (verticalMatch.size() >= 3) {
                        matchedCandies.addAll(verticalMatch);
                    }
                }
            }
        }

        if (!matchedCandies.isEmpty()) {
            // Remove matched candies and update score
            for (Entity candy : matchedCandies) {
                getGameWorld().removeEntity(candy);
            }

            // Update score
            int points = matchedCandies.size();
            score += points;
            getGameWorld().getProperties().setValue("score", score);

            // Drop new candies in place of the removed ones
            refillBoard();
        }
    }

    private void bfsMatch(int row, int col, Entity candy, Set<Entity> horizontalMatches, Set<Entity> verticalMatches) {
        // Perform a BFS flood-fill to find matching candies horizontally and vertically
        String candyType = candy.getComponent(CandyComponent.class).getCandyType();  // Access candy type from the custom component
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});

        boolean[][] visited = new boolean[GRID_SIZE][GRID_SIZE];
        visited[row][col] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];

            // Check if the candy matches the type
            if (candies[r][c].getComponent(CandyComponent.class).getCandyType().equals(candyType)) {
                // Check horizontal matches
                int horizontalCount = 1;
                int horizontalCol = c;
                while (horizontalCol > 0 && candies[r][horizontalCol - 1].getComponent(CandyComponent.class).getCandyType().equals(candyType)) {
                    horizontalCount++;
                    horizontalCol--;
                }
                horizontalCol = c;
                while (horizontalCol < GRID_SIZE - 1 && candies[r][horizontalCol + 1].getComponent(CandyComponent.class).getCandyType().equals(candyType)) {
                    horizontalCount++;
                    horizontalCol++;
                }
                if (horizontalCount >= 3) {
                    for (int i = c - horizontalCount / 2; i <= c + horizontalCount / 2; i++) {
                        horizontalMatches.add(candies[r][i]);
                    }
                }

                // Check vertical matches
                int verticalCount = 1;
                int verticalRow = r;
                while (verticalRow > 0 && candies[verticalRow - 1][c].getComponent(CandyComponent.class).getCandyType().equals(candyType)) {
                    verticalCount++;
                    verticalRow--;
                }
                verticalRow = r;
                while (verticalRow < GRID_SIZE - 1 && candies[verticalRow + 1][c].getComponent(CandyComponent.class).getCandyType().equals(candyType)) {
                    verticalCount++;
                    verticalRow++;
                }
                if (verticalCount >= 3) {
                    for (int i = r - verticalCount / 2; i <= r + verticalCount / 2; i++) {
                        verticalMatches.add(candies[i][c]);
                    }
                }

                // Check all four directions
                for (int dir = 0; dir < 4; dir++) {
                    int newRow = r + DIRECTIONS_X[dir];
                    int newCol = c + DIRECTIONS_Y[dir];

                    if (newRow >= 0 && newRow < GRID_SIZE && newCol >= 0 && newCol < GRID_SIZE &&!visited[newRow][newCol]) {
                        visited[newRow][newCol] = true;
                        queue.add(new int[]{newRow, newCol});
                    }
                }
            }
        }
    }

    private void refillBoard() {
        // Refill the grid with new random candies after a match
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (candies[i][j] == null) {
                    String candyType = candyTypes[FXGL.random(0, candyTypes.length - 1)];
                    candies[i][j] = FXGL.entityBuilder()
                            .at(i * CANDY_SIZE + 50, j * (CANDY_SIZE + 10) + 50)
                            .viewWithBBox(candyType + ".png")
                            .with(new CandyComponent(candyType))  // Attach the custom CandyComponent to the entity
                            .buildAndAttach();
                }
            }
        }
    }

    // Reset the board by randomizing the candy positions
    private void resetBoard() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Entity candyEntity = candies[i][j];

                if (candyEntity != null) {
                    // Ensure that the CandyComponent is present
                    CandyComponent candyComponent = candyEntity.getComponent(CandyComponent.class);
                    // Randomly pick a candy type
                    String candyType = candyTypes[FXGL.random(0, candyTypes.length - 1)];
                    if (candyComponent != null) {

                        // Update the candy type in the custom component
                        candyComponent.setCandyType(candyType);
                    } else {
                        System.out.println("CandyComponent is null for candy at (" + i + "," + j + ")");
                    }

                    // Ensure the ViewComponent exists and is attached
                    ViewComponent viewComponent = candyEntity.getViewComponent();
                    if (viewComponent != null) {
                        // Load the image for the candy from resources
                        javafx.scene.image.Image image = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/textures/" + candyType + ".png")));

                        // Create an ImageView for the candy
                        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);

                        // Clear existing views and add the new one
                        viewComponent.clearChildren();
                        viewComponent.addChild(imageView);
                    } else {
                        System.out.println("ViewComponent is null for candy at (" + i + "," + j + ")");
                    }

                    // Reposition the candy on the grid
                    candyEntity.setPosition(i * CANDY_SIZE + 50, j * (CANDY_SIZE + 10) + 50);
                } else {
                    System.out.println("Candy at position (" + i + "," + j + ") is null");
                }
            }
        }
    }
}
