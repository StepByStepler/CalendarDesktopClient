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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Main extends Application {
    public static Main application;
    private Stage stage;
    int id = -1;
    static double CELL_X_SIZE;
    static double CELL_Y_SIZE;
    static int PANEL_SIZE = 74;

    private Socket socket;
    BufferedReader reader;
    BufferedWriter writer;

    private Scene authorization;
    private AnchorPane authorizationPane;

    private Scene calendar;
    AnchorPane calendarPane;
    Calendar time = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

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
        CELL_Y_SIZE = (calendarPane.getPrefHeight() - PANEL_SIZE) / 8;
        CalendarController.PIXELS_IN_MINUTE = CELL_Y_SIZE / 180;
        CalendarController.PIXELS_TO_ROUND = CalendarController.PIXELS_IN_MINUTE * 15;
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

    public void launchCalendar(int id) throws IOException, ParseException {
        time.setTime(new Date());
        time.set(Calendar.MILLISECOND, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        time.setTimeInMillis(time.getTimeInMillis() - time.getTimeZone().getRawOffset());

        this.id = id;

        fillLines();
        fillCalendar();
        CalendarController.setCalendar(time);

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
            Line line = new Line(d, PANEL_SIZE, d, calendarPane.getPrefHeight());
            calendarPane.getChildren().addAll(line, label);
        }
        time.add(Calendar.DAY_OF_MONTH, -7);

        int hour = 0;
        Line separator = new Line(20, PANEL_SIZE, 20, calendarPane.getPrefHeight());
        calendarPane.getChildren().add(separator);

        for(double d = 74; d < calendarPane.getPrefHeight(); d += CELL_Y_SIZE) {
            Line line = new Line(0, d, calendarPane.getPrefWidth(), d);
            Label label = new Label(String.valueOf(hour));
            hour += 3;
            label.setLayoutY(d);
            calendarPane.getChildren().addAll(line, label);
        }
    }

    public void fillCalendar() throws IOException, ParseException {
        Calendar week = new Calendar.Builder().setInstant(time.getTimeInMillis()).build();
        long dayMillis = week.getTimeInMillis() - week.getTimeInMillis() % (24 * 60 * 60 * 1000);

        Date dateFrom = new Date(dayMillis - week.getTimeZone().getRawOffset());
        Date dateTo = new Date(dayMillis + 7 * 24 * 60 * 60 * 1000 - week.getTimeZone().getRawOffset());

        writer.write(String.format("/getdates%d~%s~%s\n", id, dateFormat.format(dateFrom), dateFormat.format(dateTo)));
        writer.flush();

        String response;
        while (!(response = reader.readLine()).equals("/end")) {
            if (response.startsWith("/date")) {
                drawPrevDates(response);
            }
        }
    }

    private void drawPrevDates(String response) throws ParseException {
        String[] args = response.replace("/date", "")
                .replace("\n", "")
                .split("~");
        Calendar from = new Calendar.Builder().setInstant(dateFormat.parse(args[0])).build();
        Calendar to = new Calendar.Builder().setInstant(dateFormat.parse(args[1])).build();
        String info = args[2];

        double startY = (from.get(Calendar.HOUR_OF_DAY) * 60 + from.get(Calendar.MINUTE)) * CELL_Y_SIZE / 180 + PANEL_SIZE;
        double endY = (to.get(Calendar.HOUR_OF_DAY) * 60 + to.get(Calendar.MINUTE)) * CELL_Y_SIZE / 180 + PANEL_SIZE;

        long diff = from.get(Calendar.DAY_OF_WEEK) - time.get(Calendar.DAY_OF_WEEK);
        if(diff < 0) {
            diff += 7;
        }
        double startX = diff * CELL_X_SIZE;
        double size = CELL_X_SIZE;

        if(from.get(Calendar.DAY_OF_MONTH) == time.get(Calendar.DAY_OF_MONTH)) {
            startX += 20;
            size -= 20;
        }

        Rectangle rect = new Rectangle(startX, startY, size, endY - startY);
        rect.getStyleClass().add("complete-rect");

        Label text = new Label(info);
        text.setLayoutX(rect.getX());
        text.setLayoutY(rect.getY());

        Label time = new Label(String.format("%02d:%02d - %02d:%02d", from.get(Calendar.HOUR_OF_DAY), from.get(Calendar.MINUTE)
                                                    , to.get(Calendar.HOUR_OF_DAY), to.get(Calendar.MINUTE)));
        time.setLayoutX(rect.getX());
        time.setLayoutY(rect.getY() + text.getFont().getSize());

        calendarPane.getChildren().addAll(rect, text, time);
    }

    public void removeCalendar() {
        calendarPane.getChildren().removeIf(node -> node.getClass() == Rectangle.class
                            || node.getClass() == Label.class
                            || node.getClass() == Line.class);
    }
}
