package utils;

import java.util.Random;
import java.util.UUID;

public class TestDataGenerator {
    private static final Random random = new Random();

    private static final String[] FIRST_NAMES = {
            "Rahul", "Priya", "Amit", "Sneha", "Vikram",
            "Ananya", "Rohan", "Neha", "Arjun", "Kavya"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Patel", "Singh", "Kumar", "Gupta",
            "Reddy", "Joshi", "Verma", "Iyer", "Nair"
    };

    private static final String[] COMPANIES = {
            "TechCorp", "InfoSys Solutions", "DataMinds", "CloudWave", "TestPro"
    };

    public static String getRandomFirstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }

    public static String getRandomLastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }

    public static String getRandomEmail() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8) + "@testmail.com";
    }

    public static String getRandomPassword() {
        return "Test@" + (1000 + random.nextInt(9000));
    }

    public static String getRandomPhone() {
        return "9" + (100000000 + random.nextInt(900000000));
    }

    public static String getRandomAddress() {
        return (1 + random.nextInt(999)) + " Test Street, Block " + (char) ('A' + random.nextInt(26));
    }

    public static String getRandomCity() {
        String[] cities = {"Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai"};
        return cities[random.nextInt(cities.length)];
    }

    public static String getRandomState() {
        String[] states = {"Maharashtra", "Karnataka", "Tamil Nadu", "Telangana", "Delhi"};
        return states[random.nextInt(states.length)];
    }

    public static String getRandomZipcode() {
        return String.valueOf(100000 + random.nextInt(899999));
    }

    public static String getRandomCompany() {
        return COMPANIES[random.nextInt(COMPANIES.length)];
    }
}
