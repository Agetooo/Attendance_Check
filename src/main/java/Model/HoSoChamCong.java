package Model;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class HoSoChamCong {

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty ten;
    private final SimpleStringProperty email;
    private final SimpleStringProperty checkin;
    private final SimpleStringProperty checkout;


    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public HoSoChamCong(int id, String ten, String email, Timestamp checkin, Timestamp checkout) {
        this.id = new SimpleIntegerProperty(id);
        this.ten = new SimpleStringProperty(ten);
        this.email = new SimpleStringProperty(email);

        this.checkin = new SimpleStringProperty(formatTimestamp(checkin));
        this.checkout = new SimpleStringProperty(formatTimestamp(checkout));
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return "---";
        }
        return ts.toLocalDateTime().format(dtf);
    }

    public int getId() { return id.get(); }
    public String getTen() { return ten.get(); }
    public String getEmail() { return email.get(); }
    public String getCheckin() { return checkin.get(); }
    public String getCheckout() { return checkout.get(); }

    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty tenProperty() { return ten; }
    public SimpleStringProperty emailProperty() { return email; }
    public SimpleStringProperty checkinProperty() { return checkin; }
    public SimpleStringProperty checkoutProperty() { return checkout; }
}
