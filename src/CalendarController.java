import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

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

    public void mousePressed(MouseEvent event) {
        double prefWidth = Main.application.calendarPane.getPrefWidth();
        currentDay = (int) (event.getX()/(prefWidth/7));
        startX = currentDay * prefWidth/7;
        endX = startX + prefWidth/7;
        startY = event.getY();
        if(currentDay == 0) {
            startX+=20;
        }
        rect = new Rectangle(startX, startY, 0, 0);
        Main.application.calendarPane.getChildren().add(rect);
    }

    public void mouseReleased(MouseEvent event) throws IOException {
        if(event.getY() > 55 && event.getX() > 20) {
            addingEvent = true;
            eventname.setDisable(false);
            addEvent.setDisable(false);
        }
    }

    public void mouseDragged(MouseEvent event) {
        endY = event.getY();
        if(endY > 55 && startY > 55) {
            rect.setWidth(endX - startX);
            rect.setHeight(endY - startY);
        }
    }

    public void addEvent(ActionEvent event) throws IOException {
        String eventName = eventname.getText();
        if(!eventName.contains("~")) {
            int startMinutes = (int) ((180 / Main.CELL_Y_SIZE) * (startY - 55));

            int endMinutes = (int) ((180 / Main.CELL_Y_SIZE) * (endY - 55));
            Main.application.writer.write(String.format("/add%d~%d~%d~%d~%s\n",
                                          Main.application.id, currentDay, startMinutes, endMinutes, eventName));
            Main.application.writer.flush();
            String response = Main.application.reader.readLine();
            switch(response) {
                case "/dateexists":
                    System.out.println("exists");
                    Main.application.calendarPane.getChildren().remove(rect);
                    break;
                case "/success":
                    System.out.println("success");
                    rect.opacityProperty().set(0.3);
                    Label text = new Label(eventName);
                    text.setLayoutX(startX);
                    text.setLayoutY((endY + startY)/2);
                    Main.application.calendarPane.getChildren().add(text);
                    break;
            }
        }
    }
}
