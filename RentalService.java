import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// All database work lives here, so Main only handles input/output.
public class RentalService {

    // ---------------- Registration & Login ----------------
    public int registerFarmer(String name, String email, String password, String phone, String village) throws SQLException {
        String sql = "INSERT INTO Farmers(name,email,password,phone,village) VALUES(?,?,?,?,?)";
        return insertUser(sql, name, email, password, phone, village);
    }

    public int registerOwner(String name, String email, String password, String phone) throws SQLException {
        String sql = "INSERT INTO Owners(name,email,password,phone) VALUES(?,?,?,?)";
        return insertUser(sql, name, email, password, phone, null);
    }

    private int insertUser(String sql, String name, String email, String pw, String phone, String village) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pw);
            ps.setString(4, phone);
            if (village != null) ps.setString(5, village);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("This email is already registered. Please login instead.");
        }
    }

    public Farmer loginFarmer(String email, String password) throws SQLException {
        String sql = "SELECT farmer_id,name,email,village FROM Farmers WHERE email=? AND password=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new Farmer(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
            }
        }
        return null;
    }

    public Owner loginOwner(String email, String password) throws SQLException {
        String sql = "SELECT owner_id,name,email FROM Owners WHERE email=? AND password=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Owner(rs.getInt(1), rs.getString(2), rs.getString(3));
            }
        }
        return null;
    }

    // ---------------- Equipment ----------------
    public int addEquipment(Owner owner, String type, String name, int horsepower, BigDecimal cuttingWidth,
                            BigDecimal rate, String location) throws SQLException {
        String sql = "INSERT INTO Equipment(owner_id,name,equipment_type,horsepower,cutting_width,daily_rate,location) " +
                     "VALUES(?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, owner.getId());
            ps.setString(2, name);
            ps.setString(3, type);
            if (type.equals("TRACTOR")) { ps.setInt(4, horsepower); ps.setNull(5, Types.DECIMAL); }
            else                        { ps.setNull(4, Types.INTEGER); ps.setBigDecimal(5, cuttingWidth); }
            ps.setBigDecimal(6, rate);
            ps.setString(7, location);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static final String EQUIPMENT_SELECT =
        "SELECT e.*, o.name AS owner_name FROM Equipment e JOIN Owners o ON e.owner_id=o.owner_id ";

    public List<Equipment> listAvailableEquipment() throws SQLException {
        return queryEquipment(EQUIPMENT_SELECT + "WHERE e.status='AVAILABLE' ORDER BY e.equipment_id", null);
    }

    public List<Equipment> listOwnerEquipment(int ownerId) throws SQLException {
        return queryEquipment(EQUIPMENT_SELECT + "WHERE e.owner_id=? ORDER BY e.equipment_id", ownerId);
    }

    private List<Equipment> queryEquipment(String sql, Integer param) throws SQLException {
        List<Equipment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("equipment_type").equals("TRACTOR")) {
                        list.add(new Tractor(rs.getInt("equipment_id"), rs.getInt("owner_id"), rs.getString("owner_name"),
                                rs.getString("name"), rs.getBigDecimal("daily_rate"), rs.getString("location"),
                                rs.getString("status"), rs.getInt("horsepower")));
                    } else {
                        list.add(new Harvester(rs.getInt("equipment_id"), rs.getInt("owner_id"), rs.getString("owner_name"),
                                rs.getString("name"), rs.getBigDecimal("daily_rate"), rs.getString("location"),
                                rs.getString("status"), rs.getBigDecimal("cutting_width")));
                    }
                }
            }
        }
        return list;
    }

    // ---------------- Booking ----------------
    // Returns the new booking id. Throws IllegalArgumentException with a friendly message on problems.
    public int bookEquipment(Farmer farmer, int equipmentId, LocalDate start, LocalDate end) throws SQLException {
        if (start.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Start date cannot be in the past.");
        if (end.isBefore(start))
            throw new IllegalArgumentException("End date cannot be before start date.");

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal rate;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT daily_rate,status FROM Equipment WHERE equipment_id=? FOR UPDATE")) {
                    ps.setInt(1, equipmentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("No equipment with ID " + equipmentId + ". Choose an ID from the list.");
                        if (!"AVAILABLE".equals(rs.getString("status")))
                            throw new IllegalArgumentException("This equipment is under maintenance right now.");
                        rate = rs.getBigDecimal("daily_rate");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COUNT(*) FROM Bookings WHERE equipment_id=? AND status IN ('PENDING','APPROVED') " +
                        "AND start_date<=? AND end_date>=?")) {
                    ps.setInt(1, equipmentId);
                    ps.setDate(2, Date.valueOf(end));
                    ps.setDate(3, Date.valueOf(start));
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0)
                            throw new IllegalArgumentException("Already booked for some of those dates. Try different dates.");
                    }
                }
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                BigDecimal total = rate.multiply(BigDecimal.valueOf(days));
                int bookingId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Bookings(farmer_id,equipment_id,start_date,end_date,total_amount) VALUES(?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, farmer.getId());
                    ps.setInt(2, equipmentId);
                    ps.setDate(3, Date.valueOf(start));
                    ps.setDate(4, Date.valueOf(end));
                    ps.setBigDecimal(5, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        bookingId = rs.getInt(1);
                    }
                }
                con.commit();
                return bookingId;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static final String RENTAL_SELECT =
        "SELECT b.booking_id, f.name AS farmer, e.name AS equipment, b.start_date, b.end_date, b.total_amount, b.status, " +
        "(SELECT COUNT(*) FROM RentalPayments p WHERE p.booking_id=b.booking_id AND p.status='SUCCESS') AS paid " +
        "FROM Bookings b JOIN Farmers f ON b.farmer_id=f.farmer_id JOIN Equipment e ON b.equipment_id=e.equipment_id ";

    public List<Rental> farmerBookings(int farmerId) throws SQLException {
        return queryRentals(RENTAL_SELECT + "WHERE b.farmer_id=? ORDER BY b.booking_id DESC", farmerId);
    }

    public List<Rental> ownerBookings(int ownerId) throws SQLException {
        return queryRentals(RENTAL_SELECT + "WHERE e.owner_id=? ORDER BY b.booking_id DESC", ownerId);
    }

    private List<Rental> queryRentals(String sql, int id) throws SQLException {
        List<Rental> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Rental(rs.getInt("booking_id"), rs.getString("farmer"), rs.getString("equipment"),
                            rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
                            rs.getBigDecimal("total_amount"), rs.getString("status"), rs.getInt("paid") > 0));
                }
            }
        }
        return list;
    }

    // Owner approves / rejects a PENDING booking on his own equipment
    public boolean decideBooking(Owner owner, int bookingId, String newStatus) throws SQLException {
        String sql = "UPDATE Bookings b JOIN Equipment e ON b.equipment_id=e.equipment_id " +
                     "SET b.status=? WHERE b.booking_id=? AND e.owner_id=? AND b.status='PENDING'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);
            ps.setInt(3, owner.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean cancelBooking(Farmer farmer, int bookingId) throws SQLException {
        String sql = "UPDATE Bookings SET status='CANCELLED' WHERE booking_id=? AND farmer_id=? AND status='PENDING'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, farmer.getId());
            return ps.executeUpdate() == 1;
        }
    }

    // ---------------- Payment ----------------
    // Amount is taken from the booking automatically - farmer doesn't type it.
    public BigDecimal pay(Farmer farmer, int bookingId, String method) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT total_amount,status FROM Bookings WHERE booking_id=? AND farmer_id=?")) {
            ps.setInt(1, bookingId);
            ps.setInt(2, farmer.getId());
            BigDecimal total;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("You have no booking with number " + bookingId + ".");
                String st = rs.getString("status");
                if (!st.equals("APPROVED") && !st.equals("COMPLETED"))
                    throw new IllegalArgumentException("You can pay only after the owner approves the booking (current status: " + st + ").");
                total = rs.getBigDecimal("total_amount");
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO RentalPayments(booking_id,amount,method) VALUES(?,?,?)")) {
                ins.setInt(1, bookingId);
                ins.setBigDecimal(2, total);
                ins.setString(3, method);
                ins.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("This booking is already paid.");
            }
            return total;
        }
    }

    // ---------------- Return & Maintenance ----------------
    public boolean markReturned(Owner owner, int bookingId) throws SQLException {
        String sql = "UPDATE Bookings b JOIN Equipment e ON b.equipment_id=e.equipment_id " +
                     "SET b.status='COMPLETED', b.returned_on=CURDATE() " +
                     "WHERE b.booking_id=? AND e.owner_id=? AND b.status='APPROVED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, owner.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean addMaintenance(Owner owner, int equipmentId, LocalDate date, String desc, BigDecimal cost) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement chk = con.prepareStatement(
                        "SELECT 1 FROM Equipment WHERE equipment_id=? AND owner_id=?")) {
                    chk.setInt(1, equipmentId);
                    chk.setInt(2, owner.getId());
                    try (ResultSet rs = chk.executeQuery()) {
                        if (!rs.next()) { con.rollback(); return false; }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO MaintenanceRecords(equipment_id,service_date,description,cost) VALUES(?,?,?,?)")) {
                    ps.setInt(1, equipmentId);
                    ps.setDate(2, Date.valueOf(date));
                    ps.setString(3, desc);
                    ps.setBigDecimal(4, cost);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Equipment SET status='MAINTENANCE' WHERE equipment_id=?")) {
                    ps.setInt(1, equipmentId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean finishMaintenance(Owner owner, int equipmentId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Equipment SET status='AVAILABLE' WHERE equipment_id=? AND owner_id=? AND status='MAINTENANCE'")) {
                    ps.setInt(1, equipmentId);
                    ps.setInt(2, owner.getId());
                    rows = ps.executeUpdate();
                }
                if (rows == 1) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE MaintenanceRecords SET status='DONE' WHERE equipment_id=? AND status='OPEN'")) {
                        ps.setInt(1, equipmentId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return rows == 1;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
