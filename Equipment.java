import java.math.BigDecimal;

// Abstract parent class. Tractor and Harvester override the type-specific parts (polymorphism).
public abstract class Equipment {
    protected final int id;
    protected final int ownerId;
    protected final String ownerName;
    protected final String name;
    protected final BigDecimal dailyRate;
    protected final String location;
    protected final String status;

    public Equipment(int id, int ownerId, String ownerName, String name,
                     BigDecimal dailyRate, String location, String status) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.name = name;
        this.dailyRate = dailyRate;
        this.location = location;
        this.status = status;
    }

    public int getId()              { return id; }
    public String getName()         { return name; }
    public BigDecimal getDailyRate(){ return dailyRate; }
    public String getLocation()     { return location; }
    public String getStatus()       { return status; }
    public String getOwnerName()    { return ownerName; }

    public abstract String getType();
    public abstract String getSpecs();

    // Child classes may override (e.g. discount for long rentals)
    public BigDecimal calculateRent(long days) {
        return dailyRate.multiply(BigDecimal.valueOf(days));
    }

    public void display() {
        System.out.printf("[%d] %-18s | %-9s | %-16s | Rs.%-8s/day | %-12s | Owner: %s | %s%n",
                id, name, getType(), getSpecs(), dailyRate, location, ownerName, status);
    }
}
