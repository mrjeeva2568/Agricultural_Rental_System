import java.math.BigDecimal;

public class Harvester extends Equipment {
    private final BigDecimal cuttingWidth;

    public Harvester(int id, int ownerId, String ownerName, String name,
                     BigDecimal rate, String location, String status, BigDecimal cuttingWidth) {
        super(id, ownerId, ownerName, name, rate, location, status);
        this.cuttingWidth = cuttingWidth;
    }

    public BigDecimal getCuttingWidth() { return cuttingWidth; }

    @Override public String getType()  { return "HARVESTER"; }
    @Override public String getSpecs() { return cuttingWidth + " ft cut"; }
}
