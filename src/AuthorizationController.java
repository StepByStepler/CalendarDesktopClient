import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorizationController {
    @FXML
    Label info;
    @FXML
    Button loginButton;
    @FXML
    Button register;
    @FXML
    TextField loginText;
    @FXML
    TextField emailText;
    @FXML
    PasswordField passwordText;

    private Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public void registerClicked(ActionEvent event) throws IOException {
        if(isEmpty()) return;

        Main.application.writer.write("/register" + loginText.getText() + " " + passwordText.getText() + " " + emailText.getText() + "\n");
        Main.application.writer.flush();
        String response = Main.application.reader.readLine();
        switch(response) {
            case "/success":
                info.setTextFill(Color.rgb(40, 255, 40));
                info.setText("Registered successfully!");
                break;
            case "/unsuccess:login":
                info.setTextFill(Color.rgb(255, 40, 40));
                info.setText("Enter correct login!");
                break;
            case "/unsuccess:password":
                info.setTextFill(Color.rgb(255, 40, 40));
                info.setText("Enter correct password!");
                break;
            case "/unsuccess:loginExists":
                info.setTextFill(Color.rgb(255, 40, 40));
                info.setText("Login already exists!");
                break;
        }
    }

    public void loginClicked(ActionEvent event) throws IOException, ParseException {
        if(isEmpty()) return;

        Main.application.writer.write("/login" + loginText.getText() + " " + passwordText.getText() + "\n");
        Main.application.writer.flush();
        String response = Main.application.reader.readLine();
        if(response.startsWith("/success")) {
            int id = Integer.parseInt(response.replace("/success", ""));
            Main.application.launchCalendar(id);
        } else if(response.startsWith("/unsuccess")) {
            info.setTextFill(Color.rgb(255, 40, 40));
            info.setText("Enter correct login and password!");
        }
    }

    private boolean isEmpty() {
        if(emailMatches(emailText.getText())) {
            info.setTextFill(Color.rgb(255, 40, 40));
            info.setText("Email not matches!");
            return true;
        }
        if(loginText.getText().isEmpty()) {
            info.setTextFill(Color.rgb(255, 40, 40));
            info.setText("Login cannot be empty!");
            return true;
        }
        if(passwordText.getText().isEmpty()) {
            info.setTextFill(Color.rgb(255, 40, 40));
            info.setText("Password cannot be empty!");
            return true;
        }
        return false;
    }

    private boolean emailMatches(String email) {
        Matcher matcher = emailPattern.matcher(email);
        return matcher.find();
    }
}
