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

    double startX, startY;
    double endX, endY;
    int currentDay;
    Rectangle rect;
    boolean addingEvent;
    double mouseX = -1, mouseY = -1;

    public void mousePressed(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        double prefWidth = Main.application.calendarPane.getPrefWidth();
        currentDay = (int) (mouseX/(prefWidth/7));
        startX = currentDay * prefWidth/7;
        endX = startX + prefWidth/7;
        startY = mouseY;
        if(currentDay == 0) {
            startX+=20;
        }
        Main.application.calendarPane.getChildren().remove(rect);
        rect = new Rectangle(startX, startY, 0, 0);
        Main.application.calendarPane.getChildren().add(rect);
    }

    public void mouseReleased(MouseEvent event) throws IOException {
        if(event.getY() > 74 && event.getX() > 20) {
            addingEvent = true;
            eventname.setDisable(false);
            addEvent.setDisable(false);
            if(rect.getHeight() > 0) {
                eventname.requestFocus();
            }
        }
    }

    public void mouseDragged(MouseEvent event) {
        endY = event.getY();
        if(endY > 74 && startY > 74) {
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
                    rect.opacityProperty().set(0.3);
                    Label text = new Label(eventName);
                    text.setLayoutX(startX);
                    text.setLayoutY((endY + startY)/2);
                    Main.application.calendarPane.getChildren().add(text);
                    rect = new Rectangle(0, 0);
                    break;
            }
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
        int reqdX = (int) mouseX;
        int reqY = (int) mouseY;
        ListIterator<Node> elements = Main.application.calendarPane.getChildren().listIterator();
        while(elements.hasNext()) {
            Node node = elements.next();
            if(node.getClass() == Rectangle.class) {
                Rectangle rectangle = (Rectangle) node;

                if(rectangle.getX() <= reqdX && rectangle.getX() + rectangle.getWidth() >= reqdX &&
                        rectangle.getY() <= reqY && rectangle.getY() + rectangle.getHeight() >= reqY) {

                    elements.remove();
                    if(elements.hasNext()) {
                        elements.next();
                        elements.remove();
                    }
                }
            }
        }
    }
}
