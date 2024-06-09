module bate.papo.msn {
    requires javafx.controls;
    requires javafx.fxml;


    opens bate.papo.msn to javafx.fxml;
    exports bate.papo.msn;
}