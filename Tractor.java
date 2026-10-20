import java.math.BigDecimal;

public class Tractor extends Equipment {
    private final int horsepower;

    public Tractor(int id, int ownerId, String ownerName, String name,
                   BigDecimal rate, String location, String status, int horsepower) {
        super(id, ownerId, ownerName, name, rate, location, status);
        this.horsepower = horsepower;
    }

    public int getHorsepower() { return horsepower; }

    @Override public String getType()  { return "TRACTOR"; }
    @Override public String getSpecs() { return horsepower + " HP"; }
}
