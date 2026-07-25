module be.condorcet.easycarrent.desktop {
	requires transitive javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;

	requires java.net.http;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;

	opens be.condorcet.easycarrent.desktop.view to javafx.fxml;
	opens be.condorcet.easycarrent.desktop.dto to com.fasterxml.jackson.databind;

	exports be.condorcet.easycarrent.desktop;
}
