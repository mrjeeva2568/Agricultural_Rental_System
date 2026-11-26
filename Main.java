import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final RentalService service = new RentalService();

    public static void main(String[] args) {
        try (Connection ignored = DBConnection.getConnection()) {
            System.out.println("Connected to MySQL successfully!");
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL: " + e.getMessage());
            System.out.println("Check the password in DBConnection.java and make sure MySQL is running.");
            return;
        }

        while (true) {
            System.out.println("\n=== AGRICULTURAL EQUIPMENT RENTAL SYSTEM ===");
            System.out.println("1. Register as Farmer");
            System.out.println("2. Register as Equipment Owner");
            System.out.println("3. Login");
            System.out.println("0. Exit");
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1: registerFarmer(); break;
                    case 2: registerOwner(); break;
                    case 3: login(); break;
                    case 0: System.out.println("Goodbye!"); return;
                    default: System.out.println("Please choose 0, 1, 2 or 3.");
                }
            } catch (SQLException e) {
                System.out.println("Database problem: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ================= Registration / Login =================
    private static void registerFarmer() throws SQLException {
        String name = ask("Your name: ");
        String email = ask("Email: ");
        String pw = ask("Choose a password: ");
        String phone = ask("Phone number: ");
        String village = ask("Village / town: ");
        require(name, email, pw);
        validateEmail(email);
        validatePhone(phone);
        int id = service.registerFarmer(name, email, pw, phone, village);
        System.out.println("\nRegistration successful! Welcome, " + name + ".");
        System.out.println("(Your Farmer ID is " + id + ", but you don't need it - just login with your email.)");
    }

    private static void registerOwner() throws SQLException {
        String name = ask("Your name: ");
        String email = ask("Email: ");
        String pw = ask("Choose a password: ");
        String phone = ask("Phone number: ");
        require(name, email, pw);
        validateEmail(email);
        validatePhone(phone);
        int id = service.registerOwner(name, email, pw, phone);
        System.out.println("\nRegistration successful! Welcome, " + name + ".");
        System.out.println("(Your Owner ID is " + id + ", but you don't need it - just login with your email.)");
    }

    private static void login() throws SQLException {
        System.out.println("Login as: 1. Farmer   2. Equipment Owner");
        int type = readInt("Choose 1 or 2: ");
        String email = ask("Email: ");
        validateEmail(email);
        String pw = ask("Password: ");
        if (type == 1) {
            Farmer f = service.loginFarmer(email, pw);
            if (f == null) { System.out.println("Wrong email or password."); return; }
            System.out.println("\nWelcome back, " + f.getName() + "!");
            farmerMenu(f);
        } else if (type == 2) {
            Owner o = service.loginOwner(email, pw);
            if (o == null) { System.out.println("Wrong email or password."); return; }
            System.out.println("\nWelcome back, " + o.getName() + "!");
            ownerMenu(o);
        } else {
            System.out.println("Please choose 1 or 2.");
        }
    }

    // ================= Farmer menu =================
    private static void farmerMenu(Farmer farmer) {
        while (true) {
            System.out.println("\n--- FARMER MENU (" + farmer.getName() + ") ---");
            System.out.println("1. Browse available equipment");
            System.out.println("2. Book equipment");
            System.out.println("3. View my bookings");
            System.out.println("4. Cancel a pending booking");
            System.out.println("5. Pay for an approved booking");
            System.out.println("0. Logout");
            int c = readInt("Choose an option: ");
            try {
                switch (c) {
                    case 1: showEquipment(service.listAvailableEquipment(), "No equipment is available right now."); break;
                    case 2: farmerBook(farmer); break;
                    case 3: showRentals(service.farmerBookings(farmer.getId()), "You have no bookings yet."); break;
                    case 4: farmerCancel(farmer); break;
                    case 5: farmerPay(farmer); break;
                    case 0: System.out.println("Logged out."); return;
                    default: System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database problem: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void farmerBook(Farmer farmer) throws SQLException {
        List<Equipment> list = service.listAvailableEquipment();
        if (!showEquipment(list, "No equipment is available right now.")) return;
        int eqId = readInt("\nEnter the number in [ ] of the equipment you want: ");
        LocalDate start = readDate("Rental start date (YYYY-MM-DD): ");
        LocalDate end = readDate("Rental end date (YYYY-MM-DD): ");
        int bookingId = service.bookEquipment(farmer, eqId, start, end);
        System.out.println("\nBooking request sent! Your booking number is " + bookingId + ".");
        System.out.println("Wait for the owner to approve it, then use 'Pay for an approved booking'.");
        for (Rental r : service.farmerBookings(farmer.getId())) { r.display(); break; }
    }

    private static void farmerCancel(Farmer farmer) throws SQLException {
        if (!showRentals(service.farmerBookings(farmer.getId()), "You have no bookings yet.")) return;
        int id = readInt("\nEnter booking number to cancel: ");
        System.out.println(service.cancelBooking(farmer, id)
                ? "Booking cancelled."
                : "Only your own PENDING bookings can be cancelled.");
    }

    private static void farmerPay(Farmer farmer) throws SQLException {
        if (!showRentals(service.farmerBookings(farmer.getId()), "You have no bookings yet.")) return;
        int id = readInt("\nEnter booking number to pay: ");
        System.out.println("Payment method: UPI / CARD / CASH");
        String method = ask("Method: ").toUpperCase();
        if (!method.equals("UPI") && !method.equals("CARD") && !method.equals("CASH"))
            throw new IllegalArgumentException("Please type UPI, CARD or CASH.");
        BigDecimal paid = service.pay(farmer, id, method);
        System.out.println("Payment of Rs." + paid + " received via " + method + ". Thank you!");
    }

    // ================= Owner menu =================
    private static void ownerMenu(Owner owner) {
        while (true) {
            System.out.println("\n--- OWNER MENU (" + owner.getName() + ") ---");
            System.out.println("1. Add new equipment");
            System.out.println("2. View my equipment");
            System.out.println("3. View booking requests for my equipment");
            System.out.println("4. Approve / Reject a booking");
            System.out.println("5. Mark equipment as returned (complete rental)");
            System.out.println("6. Send equipment for maintenance");
            System.out.println("7. Maintenance finished (make available again)");
            System.out.println("0. Logout");
            int c = readInt("Choose an option: ");
            try {
                switch (c) {
                    case 1: ownerAddEquipment(owner); break;
                    case 2: showEquipment(service.listOwnerEquipment(owner.getId()), "You have not added any equipment yet."); break;
                    case 3: showRentals(service.ownerBookings(owner.getId()), "No bookings for your equipment yet."); break;
                    case 4: ownerDecide(owner); break;
                    case 5: ownerReturn(owner); break;
                    case 6: ownerMaintenance(owner); break;
                    case 7: ownerFinishMaintenance(owner); break;
                    case 0: System.out.println("Logged out."); return;
                    default: System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database problem: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void ownerAddEquipment(Owner owner) throws SQLException {
        String[] types = {"TRACTOR", "HARVESTER", "SEED_DRILL", "PLOUGH", "ROTAVATOR", "SPRAYER", "WATER_PUMP"};
        System.out.println("Equipment type: 1. Tractor   2. Harvester   3. Seed Drill   4. Plough");
        System.out.println("                5. Rotavator  6. Sprayer    7. Water Pump");
        int t = readInt("Choose 1-7: ");
        if (t < 1 || t > types.length) throw new IllegalArgumentException("Please choose a number from 1 to 7.");
        String equipmentType = types[t - 1];
        String name = ask("Equipment name (e.g. Mahindra 575 DI): ");
        int hp = 0;
        BigDecimal width = null;
        if (equipmentType.equals("TRACTOR")) hp = readInt("Horsepower (HP): ");
        else if (equipmentType.equals("HARVESTER")) width = readMoney("Cutting width in feet: ");
        BigDecimal rate = readMoney("Rent per day (Rs.): ");
        String location = ask("Location (village/town): ");
        if (name.isEmpty() || location.isEmpty() || rate.signum() <= 0)
            throw new IllegalArgumentException("Name, location and a rent greater than 0 are required.");
        int id = service.addEquipment(owner, equipmentType, name, hp, width, rate, location);
        System.out.println("Equipment added successfully! (Equipment number " + id + ")");
    }

    private static void ownerDecide(Owner owner) throws SQLException {
        if (!showRentals(service.ownerBookings(owner.getId()), "No bookings for your equipment yet.")) return;
        int id = readInt("\nEnter booking number: ");
        System.out.println("1. Approve   2. Reject");
        int d = readInt("Choose 1 or 2: ");
        if (d != 1 && d != 2) throw new IllegalArgumentException("Please choose 1 or 2.");
        boolean ok = service.decideBooking(owner, id, d == 1 ? "APPROVED" : "REJECTED");
        System.out.println(ok ? "Booking " + (d == 1 ? "approved." : "rejected.")
                              : "No PENDING booking with that number on your equipment.");
    }

    private static void ownerReturn(Owner owner) throws SQLException {
        if (!showRentals(service.ownerBookings(owner.getId()), "No bookings for your equipment yet.")) return;
        int id = readInt("\nEnter booking number of the returned equipment: ");
        System.out.println(service.markReturned(owner, id)
                ? "Equipment returned. Rental marked COMPLETED."
                : "No APPROVED booking with that number on your equipment.");
    }

    private static void ownerMaintenance(Owner owner) throws SQLException {
        if (!showEquipment(service.listOwnerEquipment(owner.getId()), "You have not added any equipment yet.")) return;
        int id = readInt("\nEnter the equipment number [ ] to service: ");
        LocalDate date = readDate("Service date (YYYY-MM-DD): ");
        String desc = ask("What work is being done? ");
        BigDecimal cost = readMoney("Maintenance cost (Rs.): ");
        if (desc.isEmpty()) throw new IllegalArgumentException("Please describe the work.");
        System.out.println(service.addMaintenance(owner, id, date, desc, cost)
                ? "Maintenance recorded. Equipment is hidden from farmers until you mark it finished."
                : "You don't own equipment with that number.");
    }

    private static void ownerFinishMaintenance(Owner owner) throws SQLException {
        if (!showEquipment(service.listOwnerEquipment(owner.getId()), "You have not added any equipment yet.")) return;
        int id = readInt("\nEnter equipment number whose maintenance is finished: ");
        System.out.println(service.finishMaintenance(owner, id)
                ? "Equipment is AVAILABLE again."
                : "That equipment is not under maintenance (or is not yours).");
    }

    // ================= Display helpers =================
    private static boolean showEquipment(List<Equipment> list, String emptyMsg) {
        if (list.isEmpty()) { System.out.println(emptyMsg); return false; }
        System.out.println("\n--- EQUIPMENT ---");
        for (Equipment e : list) e.display();
        return true;
    }

    private static boolean showRentals(List<Rental> list, String emptyMsg) {
        if (list.isEmpty()) { System.out.println(emptyMsg); return false; }
        System.out.println("\n--- BOOKINGS ---");
        for (Rental r : list) r.display();
        return true;
    }

    // ================= Input helpers =================
    private static String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static void require(String... values) {
        for (String v : values)
            if (v.isEmpty()) throw new IllegalArgumentException("Name, email and password cannot be empty.");
    }

    private static void validateEmail(String email) {
        if (!email.toLowerCase().endsWith("@gmail.com"))
            throw new IllegalArgumentException("Email must end with @gmail.com.");
    }

    private static void validatePhone(String phone) {
        if (!phone.matches("\\d{10}"))
            throw new IllegalArgumentException("Phone number must contain exactly 10 digits.");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Please enter a number."); }
        }
    }

    private static BigDecimal readMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal v = new BigDecimal(sc.nextLine().trim());
                if (v.signum() < 0) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) { System.out.println("Please enter a valid amount (0 or more)."); }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return LocalDate.parse(sc.nextLine().trim()); }
            catch (Exception e) { System.out.println("Please use the format YYYY-MM-DD, e.g. 2026-11-05."); }
        }
    }
}
