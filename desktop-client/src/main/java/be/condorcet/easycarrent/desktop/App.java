package be.condorcet.easycarrent.desktop;

import java.net.URL;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application bootstrap for the Easy Car Rent desktop client.
 *
 * <p>This class is intentionally limited to application startup: it loads the
 * main FXML view, applies the stylesheet, and shows the primary stage. All view
 * logic belongs to {@code MainViewController}; this class contains no controls,
 * no business logic, no navigation, and no HTTP calls.</p>
 */
public class App extends Application {

	private static final String MAIN_VIEW_FXML =
			"/be/condorcet/easycarrent/desktop/view/main-view.fxml";
	private static final String APP_STYLESHEET =
			"/be/condorcet/easycarrent/desktop/view/app.css";

	private static final double INITIAL_WIDTH = 900;
	private static final double INITIAL_HEIGHT = 600;
	private static final double MIN_WIDTH = 640;
	private static final double MIN_HEIGHT = 480;

	@Override
	public void start(Stage stage) throws Exception {
		URL fxmlUrl = requireResource(MAIN_VIEW_FXML);
		URL cssUrl = requireResource(APP_STYLESHEET);

		FXMLLoader loader = new FXMLLoader(fxmlUrl);
		Parent root = loader.load();

		Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
		scene.getStylesheets().add(cssUrl.toExternalForm());

		stage.setTitle("Easy Car Rent");
		stage.setMinWidth(MIN_WIDTH);
		stage.setMinHeight(MIN_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}

	private URL requireResource(String absoluteClasspathPath) {
		return Objects.requireNonNull(
				App.class.getResource(absoluteClasspathPath),
				() -> "Required application resource not found on the classpath: "
						+ absoluteClasspathPath);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
