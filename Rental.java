import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// One booking/rental record
public class Rental {
    private final int bookingId;
    private final String farmerName;
    private final String equipmentName;
    private final LocalDate start;
    private final LocalDate end;
    private final BigDecimal total;
    private final String status;
    private final boolean paid;

    public Rental(int bookingId, String farmerName, String equipmentName, LocalDate start,
                  LocalDate end, BigDecimal total, String status, boolean paid) {
        this.bookingId = bookingId;
        this.farmerName = farmerName;
        this.equipmentName = equipmentName;
        this.start = start;
        this.end = end;
        this.total = total;
        this.status = status;
        this.paid = paid;
    }

    public long getDays() { return ChronoUnit.DAYS.between(start, end) + 1; }

    public void display() {
        System.out.printf("Booking #%d | Farmer: %s | Equipment: %s | %s to %s (%d day/s) | Rs.%s | Status: %s | Payment: %s%n",
                bookingId, farmerName, equipmentName, start, end, getDays(), total, status,
                paid ? "PAID" : "NOT PAID");
    }
}
