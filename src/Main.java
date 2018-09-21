import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ListIterator;

public class Main extends Application {
    public static Main application;
    private Stage stage;
    int id = -1;
    static double CELL_X_SIZE;
    static double CELL_Y_SIZE;

    private Socket socket;
    BufferedReader reader;
    BufferedWriter writer;

    private Scene authorization;
    private AnchorPane authorizationPane;

    private Scene calendar;
    AnchorPane calendarPane;
    Calendar time = Calendar.getInstance();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        socket = new Socket(InetAddress.getLocalHost(), 10000);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        application = this;
        this.stage = stage;
        stage.setResizable(false);
        loadScenes();
        launchAuthorization();
    }

    public void loadScenes() throws IOException {
        authorizationPane = FXMLLoader.load(getClass().getResource("Authorization.fxml"));
        authorization = new Scene(authorizationPane);

        calendarPane = FXMLLoader.load(getClass().getResource("Calendar.fxml"));
        CELL_X_SIZE = calendarPane.getPrefWidth() / 7;
        CELL_Y_SIZE = (calendarPane.getPrefHeight() - 74) / 8;
        calendar = new Scene(calendarPane);
        calendar.getStylesheets().add("style.css");
    }

    public void launchAuthorization() {
        removeCalendar();
        stage.setScene(authorization);
        stage.setTitle("Authorize");
        stage.show();
        id = -1;
    }

    public void launchCalendar(int id) throws IOException {
        time.setTime(new Date());
        this.id = id;
        fillLines();
        fillCalendar();
        stage.setScene(calendar);
        stage.setTitle("Calendar");
        stage.show();
    }

    public void fillLines() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yy, E");
        for (double d = 0; d <= calendarPane.getPrefWidth(); d += CELL_X_SIZE) {
            Label label = new Label(dateFormat.format(time.getTime()));
            label.setLayoutX(d);
            label.setLayoutY(74 - label.getFont().getSize() - 5);
            time.add(Calendar.DAY_OF_MONTH, 1);
            Line line = new Line(d, 74, d, calendarPane.getPrefHeight());
            calendarPane.getChildren().addAll(line, label);
        }
        time.add(Calendar.DAY_OF_MONTH, -7);

        int hour = 0;
        Line separator = new Line(20, 74, 20, calendarPane.getPrefHeight());
        calendarPane.getChildren().add(separator);

        for(double d = 74; d < calendarPane.getPrefHeight(); d += CELL_Y_SIZE) {
            Line line = new Line(0, d, calendarPane.getPrefWidth(), d);
            Label label = new Label(String.valueOf(hour));
            hour += 3;
            label.setLayoutY(d);
            calendarPane.getChildren().addAll(line, label);
        }
    }

    public void fillCalendar() throws IOException {
        for (int i = 0; i < 7; i++, time.add(Calendar.DAY_OF_MONTH, 1)) {
            int year = time.get(Calendar.YEAR);
            int month = time.get(Calendar.MONTH);
            int dayFrom = time.get(Calendar.DAY_OF_MONTH);
            writer.write(String.format("/getdates%d~%d~%d~%d\n", id, year, month, dayFrom));
            writer.flush();
            String response;
            while (!(response = reader.readLine()).equals("/end")) {
                if (response.startsWith("/date")) {
                    drawPrevDates(i, response);
                }
            }
        }
        time.add(Calendar.DAY_OF_MONTH, -7);
    }

    private void drawPrevDates(int i, String response) {
        String[] args = response.replace("/date", "")
                .replace("\n", "")
                .split("~");
        int minute_from = Integer.parseInt(args[0]);
        int minute_to = Integer.parseInt(args[1]);
        String info = args[2];

        double startY = minute_from * CELL_Y_SIZE / 180 + 74;
        double endY = minute_to * CELL_Y_SIZE / 180 + 74;
        double startX = i * CELL_X_SIZE;
        double size = CELL_X_SIZE;

        if(i == 0) {
            startX += 20;
            size -= 20;
        }

        Rectangle rect = new Rectangle(startX, startY, size, endY - startY);
        rect.getStyleClass().add("complete-rect");
        Label text = new Label(info);
        text.setLayoutX(startX);
        text.setLayoutY((endY + startY)/2);

        calendarPane.getChildren().addAll(rect, text);
    }

    public void removeCalendar() {
        calendarPane.getChildren().removeIf(node -> node.getClass() == Rectangle.class
                            || node.getClass() == Label.class
                            || node.getClass() == Line.class);
    }
}
