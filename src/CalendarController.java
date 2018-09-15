import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class CalendarController {
    double startX, startY;
    double endX, endY;
    Rectangle rect;

    public void mousePressed(MouseEvent event) {
        double prefWidth = Main.application.calendarPane.getPrefWidth();
        int index = (int) (event.getX()/(prefWidth/7));
        startX = index * prefWidth/7;
        endX = startX + prefWidth/7;
        startY = event.getY();
        if(index == 0) {
            startX+=20;
        }
        if(rect == null) {
            rect = new Rectangle(startX, startY, 0, 0);
            Main.application.calendarPane.getChildren().add(rect);
        } else {
            rect.setX(startX);
            rect.setY(startY);
            rect.setHeight(0);
            rect.setWidth(0);
        }
    }

    public void mouseReleased(MouseEvent event) throws IOException {
        rect.setHeight(0);
        rect.setWidth(0);
    }

    public void mouseDragged(MouseEvent event) {
        endY = event.getY();
        if(endY > 55 && startY > 55) { //50 - высота вертикальных элементов + пару пикселей
            rect.setWidth(endX - startX);
            rect.setHeight(endY - startY);
        }
    }
}
