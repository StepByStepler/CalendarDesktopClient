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
import java.util.Calendar;
import java.util.ListIterator;

public class CalendarController {
    @FXML
    TextField eventname;
    @FXML
    Button addEvent;

    public static double PIXELS_IN_MINUTE;

    private double startX, startY;
    private double endX, endY;
    private int oldTimeFrom, oldTimeTo;
    private String oldName;
    private int currentDay;
    private Rectangle rect = new Rectangle();
    private Label label;
    private boolean resizeCooldownActive;
    private double mouseX = -1, mouseY = -1;
    private boolean selectedRectIsFinal;
    private boolean shiftPressed = false;

    public void mousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();

        double prefWidth = Main.application.calendarPane.getPrefWidth();
        currentDay = (int) (mouseX / (prefWidth / 7));

        startX = currentDay * prefWidth / 7;
        endX = startX + prefWidth / 7;
        //startY = Math.round((mouseY - 74) / (15 * PIXELS_IN_MINUTE)) * (15 * PIXELS_IN_MINUTE) + 74;
        startY = roundToFifteen(mouseY);

        Rectangle copy = rect;
        selectRectangle();

        if(rect == copy) {
            if(!selectedRectIsFinal) {
                Main.application.calendarPane.getChildren().remove(rect);
            } else {
                rect.getStyleClass().remove("select-rect");
                rect.getStyleClass().add("complete-rect");
            }
            selectedRectIsFinal = false;
            if (currentDay == 0) {
                startX += 20;
            }

            rect = new Rectangle(startX, startY, 0, 0);
            label = new Label();
            rect.getStyleClass().add("select-rect");
            Main.application.calendarPane.getChildren().add(rect);
        } else {
            selectedRectIsFinal = true;
        }
    }

    public void mouseReleased(MouseEvent event) {
        if(event.getY() > 74 && event.getX() > 20) {
            eventname.setDisable(false);
            addEvent.setDisable(false);
            if(!selectedRectIsFinal && rect.getHeight() > 0) {
                eventname.requestFocus();
            }
        }
    }

    public void mouseDragged(MouseEvent event) {
        //endY = Math.round((event.getY() - 74) / (15 * PIXELS_IN_MINUTE)) * (15 * PIXELS_IN_MINUTE) + 74;
        endY = roundToFifteen(event.getY());
        if(!selectedRectIsFinal && endY > 74 && startY > 74) {
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
            int year = Main.application.time.get(Calendar.YEAR);
            int month = Main.application.time.get(Calendar.MONTH);
            if(selectedRectIsFinal) {
                System.out.println(String.format("/update%d~%d~%d~%d~%d~%d~%d~%d~%s~%s\n", Main.application.id,
                        year, month, Main.application.time.get(Calendar.DAY_OF_MONTH) + currentDay,
                        oldTimeFrom, oldTimeTo, minutesFrom, minutesTo, oldName, eventName));
                Main.application.writer.write(String.format("/update%d~%d~%d~%d~%d~%d~%d~%d~%s~%s\n", Main.application.id,
                                              year, month, Main.application.time.get(Calendar.DAY_OF_MONTH) + currentDay,
                                              oldTimeFrom, oldTimeTo, minutesFrom, minutesTo, oldName, eventName));
                Main.application.writer.flush();
                String response = Main.application.reader.readLine();
                switch(response) {
                    case "/dateexists":
                        break;
                    case "/success":
                        rect.getStyleClass().remove("select-rect");
                        rect.getStyleClass().add("complete-rect");
                        label.setText(eventName);
                        rect = new Rectangle();
                        label = new Label();
                        eventname.setDisable(true);
                        addEvent.setDisable(true);
                        break;
                }
            }
            else {
                Main.application.writer.write(String.format("/add%d~%d~%d~%d~%d~%d~%s\n", Main.application.id, year, month,
                        Main.application.time.get(Calendar.DAY_OF_MONTH) + currentDay,
                        minutesFrom, minutesTo, eventName));
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
                        text.setLayoutX(startX);
                        text.setLayoutY((endY + startY) / 2);
                        Main.application.calendarPane.getChildren().add(text);
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

    public void moveLeft(ActionEvent event) throws IOException {
        Main.application.time.add(Calendar.DAY_OF_MONTH, -7);
        Main.application.removeCalendar();
        Main.application.fillLines();
        Main.application.fillCalendar();
    }

    public void moveRight(ActionEvent event) throws IOException {
        Main.application.time.add(Calendar.DAY_OF_MONTH, 7);
        Main.application.removeCalendar();
        Main.application.fillLines();
        Main.application.fillCalendar();
    }

    public void keyPressed(KeyEvent event) throws IOException {
        if(event.getCode() == KeyCode.DELETE) {
            if(mouseX != -1) {
                int id = Main.application.id;
                int year = Main.application.time.get(Calendar.YEAR);
                int month = Main.application.time.get(Calendar.MONTH);
                int day = Main.application.time.get(Calendar.DAY_OF_MONTH) + currentDay;
                int minutes = getMinutesFromLocation(mouseY);

                Main.application.writer.write(String.format("/delete%d~%d~%d~%d~%d\n", id, year, month, day, minutes));
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
                        if(rect.getHeight() > 15 * PIXELS_IN_MINUTE) {
                            rect.setHeight(rect.getHeight() - 15 * PIXELS_IN_MINUTE);
                        }
                    } else if(rect.getY() - 15 * PIXELS_IN_MINUTE >= 74) {
                        rect.setY(roundToFifteen((rect.getY() - 15 * PIXELS_IN_MINUTE)));
                        rect.setHeight(rect.getHeight() + 15 * PIXELS_IN_MINUTE);
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
                    double newYDown = rect.getY() + rect.getHeight() + 15 * PIXELS_IN_MINUTE;

                    if (shiftPressed) {
                        if(rect.getHeight() > 15 * PIXELS_IN_MINUTE) {
                            rect.setY(roundToFifteen((rect.getY() + 15 * PIXELS_IN_MINUTE)));
                            rect.setHeight(rect.getHeight() - 15 * PIXELS_IN_MINUTE);
                        }

                    } else if (newYDown <= Main.application.calendarPane.getHeight()) {
                        rect.setHeight(rect.getHeight() + 15 * PIXELS_IN_MINUTE);
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
        return (int) Math.round(((180 / Main.CELL_Y_SIZE) * (y - 74)));
    }

    private void deleteRectangle() {
        Main.application.calendarPane.getChildren().remove(rect);
        Main.application.calendarPane.getChildren().remove(label);
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
                        label = (Label) elements.next();
                    }

                    selectedRectIsFinal = true;
                    label.requestFocus();
                    eventname.setText(label.getText());

                    oldTimeFrom = getMinutesFromLocation(rect.getY());
                    oldTimeTo = getMinutesFromLocation(rect.getY() + rect.getHeight());
                    oldName = label.getText();
                }
            }
        }
        if(copyToDelete != null) {
            Main.application.calendarPane.getChildren().remove(copyToDelete);
        }
    }

    private void recalculateLabel() {
        label.setLayoutY(rect.getY() + rect.getHeight() / 2 - 15 * PIXELS_IN_MINUTE);
    }

    private double roundToFifteen(double num) {
        return Math.round((num - 74) / (15 * PIXELS_IN_MINUTE)) * (15 * PIXELS_IN_MINUTE) + 74;
    }
}
