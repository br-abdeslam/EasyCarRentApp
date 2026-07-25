module be.condorcet.easycarrent.desktop {
	requires transitive javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;

	opens be.condorcet.easycarrent.desktop.view to javafx.fxml;

	exports be.condorcet.easycarrent.desktop;
}
