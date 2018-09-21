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

    private double startX, startY;
    private double endX, endY;
    private int currentDay;
    private Rectangle rect = new Rectangle();
    private Label label;
    private boolean addingEvent;
    private double mouseX = -1, mouseY = -1;
    private boolean selectedRectIsFinal;

    public void mousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();

        double prefWidth = Main.application.calendarPane.getPrefWidth();
        currentDay = (int) (mouseX / (prefWidth / 7));

        startX = currentDay * prefWidth / 7;
        endX = startX + prefWidth / 7;
        startY = mouseY;

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

//            rect.getStyleClass().remove("select-rect");
//            rect.getStyleClass().add("complete-rect");
            rect = new Rectangle(startX, startY, 0, 0);
            rect.getStyleClass().add("select-rect");
            Main.application.calendarPane.getChildren().add(rect);
        } else {
            selectedRectIsFinal = true;
        }
    }

    public void mouseReleased(MouseEvent event) throws IOException {
        if(event.getY() > 74 && event.getX() > 20) {
            addingEvent = true;
            eventname.setDisable(false);
            addEvent.setDisable(false);
            if(!selectedRectIsFinal && rect.getHeight() > 0) {
                eventname.requestFocus();
            }
        }
    }

    public void mouseDragged(MouseEvent event) {
        endY = event.getY();
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
            int startMinutes = getMinutesFromLocation(startY);
            int endMinutes = getMinutesFromLocation(endY);
            int year = Main.application.time.get(Calendar.YEAR);
            int month = Main.application.time.get(Calendar.MONTH);
            Main.application.writer.write(String.format("/add%d~%d~%d~%d~%d~%d~%s\n", Main.application.id, year, month,
                                                 Main.application.time.get(Calendar.DAY_OF_MONTH) + currentDay,
                                                 startMinutes, endMinutes, eventName));
            Main.application.writer.flush();
            String response = Main.application.reader.readLine();
            switch(response) {
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
                    text.setLayoutY((endY + startY)/2);
                    Main.application.calendarPane.getChildren().add(text);
                    rect = new Rectangle(0, 0);
                    break;
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
    }

    private int getMinutesFromLocation(double y) {
        return (int) ((180 / Main.CELL_Y_SIZE) * (y - 74));
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
                    label.requestFocus(); //remove focus from textfield;
                }
            }
        }
        if(copyToDelete != null) {
            Main.application.calendarPane.getChildren().remove(copyToDelete);
        }
    }
}
