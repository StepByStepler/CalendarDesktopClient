import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ListIterator;
import java.util.TimeZone;

public class CalendarController {
    @FXML
    TextField eventname;
    @FXML
    Button addEvent;

    public static double PIXELS_IN_MINUTE;
    private static int MINUTES_TO_ROUND = 15;
    static double PIXELS_TO_ROUND = PIXELS_IN_MINUTE * MINUTES_TO_ROUND;
    private static Calendar calendar;

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private double startX, startY;
    private double endX, endY;
    private int oldTimeFrom, oldTimeTo;
    private String oldName;
    private int currentDay;
    private Rectangle rect = new Rectangle();
    private Label name;
    private Label time;
    private boolean resizeCooldownActive;
    private double mouseX = -1, mouseY = -1;
    private boolean selectedRectIsFinal;
    private boolean shiftPressed = false;

    public static void setCalendar(Calendar calendar) {
        CalendarController.calendar = calendar;
    }

    public void mousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();

        double prefWidth = Main.application.calendarPane.getPrefWidth();
        currentDay = (int) (mouseX / (prefWidth / 7));

        startX = currentDay * prefWidth / 7;
        endX = startX + prefWidth / 7;
        startY = roundToFifteen(mouseY);

        Rectangle copy = rect;
        selectRectangle();

        if(rect == copy) {
            if(!selectedRectIsFinal) {
                Main.application.calendarPane.getChildren().remove(rect);
            } else {
                resetDate();
                rect.getStyleClass().remove("select-rect");
                rect.getStyleClass().add("complete-rect");
            }
            selectedRectIsFinal = false;
            if (currentDay == 0) {
                startX += 20;
            }

            rect = new Rectangle(startX, startY, 0, 0);
            name = new Label();
            time = new Label();
            rect.getStyleClass().add("select-rect");
            Main.application.calendarPane.getChildren().add(rect);
        } else {
            selectedRectIsFinal = true;
        }
    }

    public void mouseReleased(MouseEvent event) {
        if(event.getY() > Main.PANEL_SIZE && event.getX() > 20) {
            eventname.setDisable(false);
            addEvent.setDisable(false);
            if(!selectedRectIsFinal && rect.getHeight() > 0) {
                eventname.requestFocus();
            }
        }
    }

    public void mouseDragged(MouseEvent event) {
        endY = roundToFifteen(event.getY());
        if(!selectedRectIsFinal && endY > Main.PANEL_SIZE && startY > Main.PANEL_SIZE) {
            rect.setWidth(endX - startX);
            rect.setHeight(endY - startY);
        }
    }

    public void addEvent(ActionEvent event) throws IOException {
        if(rect.getHeight() == 0) {
            return;
        }
        String eventName = eventname.getText();
        if(!eventName.contains("~")) {
            int minutesFrom = getMinutesFromLocation(rect.getY());
            int minutesTo = getMinutesFromLocation(rect.getY() + rect.getHeight());
            if(selectedRectIsFinal) {

                Calendar oldDateFrom = buildCalendarFromCurrent(currentDay, oldTimeFrom);
                Calendar oldDateTo = buildCalendarFromCurrent(currentDay, oldTimeTo);

                Calendar newDateFrom = buildCalendarFromCurrent(currentDay, minutesFrom);
                Calendar newDateTo = buildCalendarFromCurrent(currentDay, minutesTo);

                Main.application.writer.write(String.format("/update%d~%s~%s~%s~%s~%s~%s\n", Main.application.id,
                                        dateFormat.format(oldDateFrom.getTime()), dateFormat.format(oldDateTo.getTime()),
                                        dateFormat.format(newDateFrom.getTime()), dateFormat.format(newDateTo.getTime()),
                                        oldName, eventName));
                Main.application.writer.flush();

                String response = Main.application.reader.readLine();
                switch(response) {
                    case "/dateexists":
                        rect.getStyleClass().remove("select-rect");
                        rect.getStyleClass().add("complete-rect");
                        resetDate();
                        break;
                    case "/success":
                        rect.getStyleClass().remove("select-rect");
                        rect.getStyleClass().add("complete-rect");
                        name.setText(eventName);
                        rect = new Rectangle();
                        name = new Label();
                        time = new Label();
                        eventname.setDisable(true);
                        addEvent.setDisable(true);
                        break;
                }
            }
            else {
                long dayMillis = calendar.getTime().getTime()
                                - calendar.getTime().getTime() % 86400000;

                Date dateFrom = new Date(dayMillis + (currentDay + 1) * 1000 * 60 * 60 * 24 + minutesFrom * 60 * 1000);
                Date dateTo = new Date(dayMillis + (currentDay + 1) * 1000 * 60 * 60 * 24 + minutesTo * 60 * 1000);

                Main.application.writer.write(String.format("/add%d~%s~%s~%s\n", Main.application.id, dateFormat.format(dateFrom),
                        dateFormat.format(dateTo), eventName));
                Calendar.getInstance();
                Main.application.writer.flush();

                String response = Main.application.reader.readLine();
                switch (response) {
                    case "/dateexists":
                        Main.application.calendarPane.getChildren().remove(rect);
                        break;
                    case "/incorrect":
                        Main.application.calendarPane.getChildren().remove(rect);
                        break;
                    case "/success":
                        rect.getStyleClass().remove("select-rect");
                        rect.getStyleClass().add("complete-rect");
                        Label text = new Label(eventName);
                        Label time = new Label(formatTime(rect));
                        text.setLayoutX(rect.getX());
                        text.setLayoutY(rect.getY());
                        time.setLayoutX(rect.getX());
                        time.setLayoutY(rect.getY() + text.getFont().getSize());
                        Main.application.calendarPane.getChildren().add(text);
                        Main.application.calendarPane.getChildren().add(time);
                        rect = new Rectangle(0, 0);
                        break;
                }
            }
            eventname.setDisable(true);
            addEvent.setDisable(true);
            eventname.clear();
        }
    }

    public void logOut(ActionEvent event) {
        Main.application.calendarPane.getChildren().remove(rect);
        eventname.clear();
        Main.application.launchAuthorization();
    }

    public void moveLeft(ActionEvent event) throws IOException, ParseException {
        Main.application.time.add(Calendar.DAY_OF_MONTH, -7);
        Main.application.removeCalendar();
        Main.application.fillLines();
        Main.application.fillCalendar();
    }

    public void moveRight(ActionEvent event) throws IOException, ParseException {
        Main.application.time.add(Calendar.DAY_OF_MONTH, 7);
        Main.application.removeCalendar();
        Main.application.fillLines();
        Main.application.fillCalendar();
    }

    public void keyPressed(KeyEvent event) throws IOException {
        if(event.getCode() == KeyCode.DELETE) {
            if(mouseX != -1 && selectedRectIsFinal) {
                int id = Main.application.id;

                Calendar deletingFrom = new Calendar.Builder().setInstant(calendar.getTimeInMillis()).build();
                deletingFrom.add(Calendar.DAY_OF_MONTH, currentDay);
                deletingFrom.add(Calendar.MINUTE, getMinutesFromLocation(rect.getY()));

                Calendar deletingTo = new Calendar.Builder().setInstant(calendar.getTimeInMillis()).build();
                deletingTo.add(Calendar.DAY_OF_MONTH, currentDay);
                deletingTo.add(Calendar.MINUTE, getMinutesFromLocation(rect.getY() + rect.getHeight()));

                Main.application.writer.write(String.format("/delete%d~%s~%s\n", id,
                        dateFormat.format(deletingFrom.getTime()), dateFormat.format(deletingTo.getTime())));
                Main.application.writer.flush();

                String result = Main.application.reader.readLine();
                if(result.equals("/changed")) {
                    deleteRectangle();
                }
            }
        }
        else if(event.getCode() == KeyCode.UP) {
            if(!resizeCooldownActive) {
                if (selectedRectIsFinal) {

                    if (shiftPressed) {
                        if(rect.getHeight() > PIXELS_TO_ROUND) {
                            rect.setHeight(rect.getHeight() - PIXELS_TO_ROUND);
                        }
                    } else if(rect.getY() - PIXELS_TO_ROUND >= Main.PANEL_SIZE) {
                        rect.setY(roundToFifteen((rect.getY() - PIXELS_TO_ROUND)));
                        rect.setHeight(rect.getHeight() + PIXELS_TO_ROUND);
                    }
                }
                recalculateLabel();
                resizeCooldownActive = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resizeCooldownActive = false;
                }).start();
            }
        }
        else if(event.getCode() == KeyCode.DOWN) {
            if(!resizeCooldownActive) {
                if (selectedRectIsFinal) {
                    double newYDown = rect.getY() + rect.getHeight() + PIXELS_TO_ROUND;

                    if (shiftPressed) {
                        if(rect.getHeight() > PIXELS_TO_ROUND) {
                            rect.setY(roundToFifteen((rect.getY() + PIXELS_TO_ROUND)));
                            rect.setHeight(rect.getHeight() - PIXELS_TO_ROUND);
                        }

                    } else if (newYDown <= Main.application.calendarPane.getHeight()) {
                        rect.setHeight(rect.getHeight() + PIXELS_TO_ROUND);
                    }
                }
                recalculateLabel();
                resizeCooldownActive = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resizeCooldownActive = false;
                }).start();
            }
        }
        else if(event.getCode() == KeyCode.SHIFT) {
            shiftPressed = true;
        }
    }

    public void keyReleased(KeyEvent event) {
        if(event.getCode() == KeyCode.SHIFT) {
            shiftPressed = false;
        }
    }

    private int getMinutesFromLocation(double y) {
        return (int) Math.round((180 / Main.CELL_Y_SIZE) * (y - Main.PANEL_SIZE));
    }

    private void deleteRectangle() {
        Main.application.calendarPane.getChildren().remove(rect);
        Main.application.calendarPane.getChildren().remove(name);
        Main.application.calendarPane.getChildren().remove(time);
    }

    private void selectRectangle() {
        ListIterator<Node> elements = Main.application.calendarPane.getChildren().listIterator();
        Rectangle copyToDelete = null;
        while(elements.hasNext()) {
            Node node = elements.next();
            if(node.getClass() == Rectangle.class) {
                Rectangle rectangle = (Rectangle) node;


                if(rectangle.getX() <= mouseX && rectangle.getX() + rectangle.getWidth() >= mouseX &&
                            rectangle.getY() <= mouseY && rectangle.getY() + rectangle.getHeight() >= mouseY) {

                    if(selectedRectIsFinal) {
                        rect.getStyleClass().remove("select-rect");
                        rect.getStyleClass().add("complete-rect");
                    } else {
                        copyToDelete = rect; //anti ConcurrentModificationException
                    }
                    rect = rectangle;
                    rect.getStyleClass().add("select-rect");

                    if(elements.hasNext()) {
                        name = (Label) elements.next();
                    }
                    if(elements.hasNext()) {
                        time = (Label) elements.next();
                    }

                    selectedRectIsFinal = true;
                    name.requestFocus();
                    eventname.setText(name.getText());

                    oldTimeFrom = getMinutesFromLocation(rect.getY());
                    oldTimeTo = getMinutesFromLocation(rect.getY() + rect.getHeight());
                    oldName = name.getText();
                }
            }
        }
        if(copyToDelete != null) {
            Main.application.calendarPane.getChildren().remove(copyToDelete);
        }
    }

    private void recalculateLabel() {
        name.setLayoutY(rect.getY());

        time.setLayoutY(rect.getY() + name.getFont().getSize());
        time.setText(formatTime(rect));
        //name.setLayoutY(rect.getY() + rect.getHeight() / 2 - 15 * PIXELS_IN_MINUTE);
    }

    private double roundToFifteen(double num) {
        return Math.round((num - Main.PANEL_SIZE) / PIXELS_TO_ROUND) * PIXELS_TO_ROUND + Main.PANEL_SIZE;
    }

    private String formatTime(Rectangle rect) {
        int minuteFrom = getMinutesFromLocation(rect.getY());
        int minuteTo = getMinutesFromLocation(rect.getY() + rect.getHeight());
        int hourFrom = minuteFrom / 60;
        int hourTo = minuteTo / 60;
        minuteFrom = minuteFrom % 60;
        minuteTo = minuteTo % 60;
        return String.format("%02d:%02d - %02d:%02d", hourFrom, minuteFrom, hourTo, minuteTo);
    }

    private void resetDate() {
        rect.setY(roundToFifteen(oldTimeFrom * PIXELS_IN_MINUTE + Main.PANEL_SIZE));
        rect.setHeight((oldTimeTo - oldTimeFrom) * PIXELS_IN_MINUTE);
        name.setText(oldName);
        recalculateLabel();
    }

    private Calendar buildCalendarFromCurrent(int day, int minutes) {
        Calendar time = new Calendar.Builder().setInstant(calendar.getTimeInMillis()).build();
        time.add(Calendar.DAY_OF_MONTH, day);
        time.add(Calendar.MINUTE, minutes);
        return time;
    }
}
