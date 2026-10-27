import java.sql.*;
import java.util.Scanner;

public class PatientRecords {

    static Connection con;
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/company";
        String user = "root";
        String password = "your_password";

        try {
            con = DriverManager.getConnection(url, user, password);

            int choice;
            do {
                System.out.println("\n--- Hospital Patient Records ---");
                System.out.println("1. Register patient");
                System.out.println("2. Add doctor");
                System.out.println("3. Book appointment");
                System.out.println("4. Search patient by ID");
                System.out.println("5. Appointments for a doctor");
                System.out.println("6. View all patients");
                System.out.println("7. Exit");
                System.out.print("Enter choice: ");

                try {
                    choice = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    choice = 0;
                }

                try {
                    switch (choice) {
                        case 1: registerPatient(); break;
                        case 2: addDoctor(); break;
                        case 3: bookAppointment(); break;
                        case 4: searchPatient(); break;
                        case 5: doctorAppointments(); break;
                        case 6: viewPatients(); break;
                        case 7: System.out.println("Goodbye."); break;
                        default: System.out.println("Invalid choice.");
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Not saved: duplicate ID, double booking, "
                        + "or an ID that does not exist.");
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid date or time format.");
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            } while (choice != 7);

            con.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    static void registerPatient() throws SQLException {
        System.out.print("Patient ID: ");
        int id = Integer.parseInt(sc.nextLine());
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Age: ");
        int age = Integer.parseInt(sc.nextLine());
        System.out.print("Contact number: ");
        String contact = sc.nextLine().trim();

        if (name.isEmpty() || contact.isEmpty()) {
            System.out.println("Name and contact cannot be empty.");
            return;
        }
        if (age < 0 || age > 120) {
            System.out.println("Enter a realistic age (0-120).");
            return;
        }

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO patients VALUES (?, ?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setInt(3, age);
        ps.setString(4, contact);
        ps.executeUpdate();
        System.out.println("Patient registered.");
    }

    static void addDoctor() throws SQLException {
        System.out.print("Doctor ID: ");
        int id = Integer.parseInt(sc.nextLine());
        System.out.print("Doctor name: ");
        String name = sc.nextLine().trim();
        System.out.print("Specialization: ");
        String spec = sc.nextLine().trim();

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO doctors VALUES (?, ?, ?)");
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, spec);
        ps.executeUpdate();
        System.out.println("Doctor added.");
    }

  
    static void bookAppointment() throws SQLException {
        System.out.print("Patient ID: ");
        int patientId = Integer.parseInt(sc.nextLine());
        showDoctors();
        System.out.print("Doctor ID: ");
        int doctorId = Integer.parseInt(sc.nextLine());
        System.out.print("Date (yyyy-mm-dd): ");
        String date = sc.nextLine().trim();
        System.out.print("Time (HH:mm): ");
        String time = sc.nextLine().trim() + ":00";
        System.out.print("Reason: ");
        String reason = sc.nextLine().trim();

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO patient_appointments " +
            "(patient_id, doctor_id, appt_date, appt_time, reason) VALUES (?, ?, ?, ?, ?)");
        ps.setInt(1, patientId);
        ps.setInt(2, doctorId);
        ps.setDate(3, Date.valueOf(date));
        ps.setTime(4, Time.valueOf(time));
        ps.setString(5, reason);
        ps.executeUpdate();
        System.out.println("Appointment booked.");
    }

    static void searchPatient() throws SQLException {
        System.out.print("Enter patient ID: ");
        int id = Integer.parseInt(sc.nextLine());

        PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM patients WHERE patient_id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("No patient found with ID " + id);
            return;
        }
        System.out.println("\nPatient ID : " + rs.getInt("patient_id"));
        System.out.println("Name       : " + rs.getString("patient_name"));
        System.out.println("Age        : " + rs.getInt("age"));
        System.out.println("Contact    : " + rs.getString("contact"));

        PreparedStatement ap = con.prepareStatement(
            "SELECT a.appt_id, d.doctor_name, a.appt_date, a.appt_time, a.reason " +
            "FROM patient_appointments a JOIN doctors d ON a.doctor_id = d.doctor_id " +
            "WHERE a.patient_id = ? ORDER BY a.appt_date, a.appt_time");
        ap.setInt(1, id);
        ResultSet ars = ap.executeQuery();

        System.out.println("\nAppointments:");
        System.out.printf("%-6s %-14s %-12s %-8s %s%n",
            "ID", "Doctor", "Date", "Time", "Reason");
        int count = 0;
        while (ars.next()) {
            System.out.printf("%-6d %-14s %-12s %-8s %s%n",
                ars.getInt("appt_id"), ars.getString("doctor_name"),
                ars.getDate("appt_date"), ars.getTime("appt_time"),
                ars.getString("reason"));
            count++;
        }
        if (count == 0) System.out.println("No appointments.");
    }

    
    static void doctorAppointments() throws SQLException {
        showDoctors();
        System.out.print("Select doctor ID: ");
        int doctorId = Integer.parseInt(sc.nextLine());

        PreparedStatement ps = con.prepareStatement(
            "SELECT a.appt_id, p.patient_id, p.patient_name, a.appt_date, " +
            "a.appt_time, a.reason " +
            "FROM patient_appointments a JOIN patients p ON a.patient_id = p.patient_id " +
            "WHERE a.doctor_id = ? ORDER BY a.appt_date, a.appt_time");
        ps.setInt(1, doctorId);
        ResultSet rs = ps.executeQuery();

        System.out.printf("%n%-6s %-10s %-12s %-12s %-8s %s%n",
            "ID", "Patient ID", "Name", "Date", "Time", "Reason");
        int count = 0;
        while (rs.next()) {
            System.out.printf("%-6d %-10d %-12s %-12s %-8s %s%n",
                rs.getInt("appt_id"), rs.getInt("patient_id"),
                rs.getString("patient_name"), rs.getDate("appt_date"),
                rs.getTime("appt_time"), rs.getString("reason"));
            count++;
        }
        if (count == 0) System.out.println("No appointments for this doctor.");
    }

    static void viewPatients() throws SQLException {
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT * FROM patients ORDER BY patient_id");
        System.out.printf("%-8s %-12s %-5s %s%n", "ID", "Name", "Age", "Contact");
        int count = 0;
        while (rs.next()) {
            System.out.printf("%-8d %-12s %-5d %s%n",
                rs.getInt("patient_id"), rs.getString("patient_name"),
                rs.getInt("age"), rs.getString("contact"));
            count++;
        }
        if (count == 0) System.out.println("No patients registered.");
    }

    static void showDoctors() throws SQLException {
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT * FROM doctors ORDER BY doctor_id");
        System.out.println("Doctors:");
        while (rs.next()) {
            System.out.println("  " + rs.getInt("doctor_id") + " - "
                + rs.getString("doctor_name") + " ("
                + rs.getString("specialization") + ")");
        }
    }
}