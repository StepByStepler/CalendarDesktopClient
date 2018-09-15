import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
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

    private Socket socket;
    public BufferedReader reader;
    public BufferedWriter writer;

    public Scene authorization;
    public AnchorPane authorizationPane;

    public Scene calendar;
    public AnchorPane calendarPane;

    public static void main(String[] args) {
//        new Thread(() -> {
//            try {
//                while(true) {
//                    Thread.sleep(5000);
//                    System.out.println(Runtime.getRuntime().freeMemory());
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
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
        for (double d = calendarPane.getPrefWidth()/7; d < calendarPane.getPrefWidth(); d += calendarPane.getPrefWidth()/7) {
            Line line = new Line(d, 55, d, calendarPane.getPrefHeight());
            calendarPane.getChildren().add(line);
        }

        int hour = 0;
        Line separator = new Line(20, 55, 20, calendarPane.getPrefHeight()); //separator between hours and table
        calendarPane.getChildren().add(separator);

        for(double d = 55; d < calendarPane.getPrefHeight(); d += (calendarPane.getPrefHeight() - 55) / 8) {
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

    public void launchCalendar(int id) {
        stage.setScene(calendar);
        stage.setTitle("Calendar");
        stage.show();
        this.id = id;
    }
}
