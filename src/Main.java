import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Main extends Application {
    public static Main application;
    public Stage stage;
    public int id = -1;
    public static double CELL_X_SIZE;
    public static double CELL_Y_SIZE;

    private Socket socket;
    public BufferedReader reader;
    public BufferedWriter writer;

    public Scene authorization;
    public AnchorPane authorizationPane;

    public Scene calendar;
    public AnchorPane calendarPane;

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
        CELL_Y_SIZE = (calendarPane.getPrefHeight() - 55) / 8;

        for (double d = calendarPane.getPrefWidth()/7; d < calendarPane.getPrefWidth(); d += CELL_X_SIZE) {
            Line line = new Line(d, 55, d, calendarPane.getPrefHeight());
            calendarPane.getChildren().add(line);
        }

        int hour = 0;
        Line separator = new Line(20, 55, 20, calendarPane.getPrefHeight());
        calendarPane.getChildren().add(separator);

        for(double d = 55; d < calendarPane.getPrefHeight(); d += CELL_Y_SIZE) {
            Line line = new Line(0, d, calendarPane.getPrefWidth(), d);
            Label label = new Label(String.valueOf(hour));
            hour += 3;
            label.setLayoutY(d);
            calendarPane.getChildren().addAll(line, label);
        }
        calendar = new Scene(calendarPane);
    }

    public void launchAuthorization() {
        stage.setScene(authorization);
        stage.setTitle("Authorize");
        stage.show();
        id = -1;
    }

    public void launchCalendar(int id) throws IOException {
        writer.write("/getdates" + id + "\n");
        writer.flush();
        String response;
        while(!(response = reader.readLine()).equals("/end")) {
            if(response.startsWith("/date")) {
                System.out.println("received");
                drawPrevDates(response);
            }
        }
        stage.setScene(calendar);
        stage.setTitle("Calendar");
        stage.show();
        this.id = id;
    }


    private void drawPrevDates(String response) {
        String[] args = response.replace("/date", "")
                .replace("\n", "")
                .split("~");
        int day = Integer.parseInt(args[0]);
        int minute_from = Integer.parseInt(args[1]);
        int minute_to = Integer.parseInt(args[2]);
        String info = args[3];

        double startY = minute_from * CELL_Y_SIZE / 180 + 55;
        double endY = minute_to * CELL_Y_SIZE / 180 + 55;
        double startX = day * CELL_X_SIZE;
        if(day == 0) {
            startX += 20;
        }

        Rectangle rect = new Rectangle(startX, startY, CELL_X_SIZE, endY - startY);
        Label text = new Label(info);
        text.setLayoutX(startX);
        text.setLayoutY(startY + startY / 2);

        calendarPane.getChildren().addAll(rect, text);
    }
}
