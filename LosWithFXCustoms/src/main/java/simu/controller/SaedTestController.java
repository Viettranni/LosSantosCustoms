package simu.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import simu.framework.Clock;
import simu.framework.Trace;
import javafx.application.Platform;
import simu.model.Customer;

import java.util.*;

public class SaedTestController {
    public TextField dealerShipName;
    @FXML private TextField meanPrice;
    @FXML private TextField priceVariance;
    @FXML private TextField amountOfCars;
    @FXML private Slider arrivalMeanSlider;
    @FXML private Label arrivalMeanLabel;
    @FXML private Slider arrivalVarianceSlider;
    @FXML private Label arrivalVarianceLabel;
    @FXML private Slider financeMeanSlider;
    @FXML private Label financeMeanLabel;
    @FXML private Slider financeVarianceSlider;
    @FXML private Label financeVarianceLabel;
    @FXML private Slider closureMeanSlider;
    @FXML private Label closureMeanLabel;
    @FXML private Slider closureVarianceSlider;
    @FXML private Label closureVarianceLabel;
    @FXML private Slider testDriveMeanSlider;
    @FXML private Label testDriveMeanLabel;
    @FXML private Slider testDriveVarianceSlider;
    @FXML private Label testDriveVarianceLabel;
    @FXML private Spinner<Integer> arrivalServicePoints;
    @FXML private Spinner<Integer> financeServicePoints;
    @FXML private Spinner<Integer> closureServicePoints;
    @FXML private ComboBox<String> carType;
    @FXML private ComboBox<String> fuelType;
    @FXML private ComboBox<String> carSets;
    @FXML private TextField basePrice;
    @FXML private Slider priceAdjustmentSlider;
    @FXML private Label adjustedPriceLabel;
    @FXML private TableView<String[]> carTable;
    @FXML private TableColumn<String[], String> carAmountColumn;
    @FXML private TableColumn<String[], String> carTypeColumn;
    @FXML private TableColumn<String[], String> carFuelTypeColumn;
    @FXML private TableColumn<String[], String> carMeanPriceColumnn;
    @FXML private TableColumn<String[], String> carPriceVarianceColumn;
    @FXML private TableColumn<String[], Button> carActionColumn;
    @FXML private Slider simulationSpeedSlider;
    @FXML private Label simulationSpeedLabel;
    @FXML private TextArea consoleLog;
    @FXML private Label consoleLogLabel;
    private SimuController simuController = new SimuController();
    private Thread simulationThread;
    private String tableName;
    ArrayList<String[]> carsToBeCreated = new ArrayList<>();
    int simulationSpeed;
    String dataBaseTableName;

    private ObservableList<Car> cars = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupSliders();
        setupCarTypeListener();
        setupCarSetsListener();
        setupCarTypes();
        setUpFuelTypes();
        setupCarSets();
        setupCarTable();
        //setupPriceAdjustment();
    }

    private void setupSliders() {
        setupSlider(arrivalMeanSlider, arrivalMeanLabel, " minutes");
        setupSlider(arrivalVarianceSlider, arrivalVarianceLabel, "");
        setupSlider(financeMeanSlider, financeMeanLabel, " minutes");
        setupSlider(financeVarianceSlider, financeVarianceLabel, "");
        setupSlider(closureMeanSlider, closureMeanLabel, " minutes");
        setupSlider(closureVarianceSlider, closureVarianceLabel, "");
        setupSlider(testDriveMeanSlider, testDriveMeanLabel, " minutes");
        setupSlider(testDriveVarianceSlider, testDriveVarianceLabel, "");
        setupSlider(simulationSpeedSlider, simulationSpeedLabel, "x");
    }

    private void setupSlider(Slider slider, Label label, String suffix) {
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                label.setText(String.format("%.1f%s", newVal.doubleValue(), suffix)));
    }

    private void setupCarTypeListener() {
        // Add a listener to carSets (ComboBox)
        carType.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Only update if the value actually changed
            HashMap<String, Double> basePrices = simuController.getBasePrices();
            if (newValue != null && !newValue.equals(oldValue)) {
                basePrice.setText(String.valueOf(basePrices.get(newValue.toLowerCase())));
            }
        });
    }

    private void setupCarSetsListener() {
        // Add a listener to carSets (ComboBox)
        carSets.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Only update if the value actually changed
            if (newValue != null && !newValue.equals(oldValue)) {
                getCarsFromDb();  // Fetch the new cars based on the selected car type
                populateTable();  // Update the table with the new data
                consoleLog.appendText("Car combinations to be created: " + carsToBeCreated.size() + "\n");
            }
        });
    }

    private void setupCarTypes() {carType.getItems().addAll("Van", "Compact", "Sedan", "SUV", "Sport");}

    private void setUpFuelTypes() {fuelType.getItems().addAll("Gas", "Hybrid", "Electric");}

    // THIS WILL BE CHANGED LATER
    private void setupCarSets() {carSets.getItems().addAll(simuController.getTableNames());}

    // THIS WILL BE CHANGED LATER
    private void setupCarTable() {
        // Set cell value factories to point to specific columns in the data
        carAmountColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()[0])); // Car Amount (index 0)
        carTypeColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()[1])); // Car Type (index 1)
        carFuelTypeColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()[2])); // Fuel Type (index 2)
        carMeanPriceColumnn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()[3])); // Mean Price (index 3)
        carPriceVarianceColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()[4])); // Price variance (index 4)
        // Add delete button to the action column
        // Add delete button to the action column
        carActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                // Configure delete button
                deleteButton.setOnAction(event -> {
                    int selectedIndex = getIndex(); // Get current row index
                    if (selectedIndex >= 0) {
                        // Remove row from the table
                        String[] removedRow = carTable.getItems().remove(selectedIndex);

                        // Remove from the underlying ArrayList
                        carsToBeCreated.remove(removedRow);
                        consoleLog.appendText("Car combinations to be created: " + carsToBeCreated.size() + "\n");
                    }
                });
            }

            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null); // Clear cell content for empty rows
                } else {
                    setGraphic(deleteButton); // Show delete button
                }
            }
        });

        // Load data from the database and populate the table
        getCarsFromDb();  // Call the method to fetch data
        populateTable();  // Populate TableView with fetched data
        consoleLog.appendText("Car combinations to be created: " + carsToBeCreated.size() + "\n");
    }


    // THIS WILL BE CHANGED LATER
    private void populateTable() {
        // Convert ArrayList<String[]> to ObservableList<String[]>
        ObservableList<String[]> tableData = FXCollections.observableArrayList(carsToBeCreated);

        // Set the data in the TableView
        carTable.setItems(tableData);
    }



    private void setupPriceAdjustment() {
        priceAdjustmentSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double basePrice = Double.parseDouble(this.basePrice.getText());
            double adjustedPrice = basePrice * (1 + newVal.doubleValue() / 100);
            adjustedPriceLabel.setText(String.format("Adjusted Price: $%.2f", adjustedPrice));
        });
    }

    @FXML
    private void handleAddCar() {
        String amount = amountOfCars.getText();
        String type = carType.getValue();
        String carFuelType = fuelType.getValue();
        String carMeanPrice = meanPrice.getText();
        String carPriceVariance = priceVariance.getText();
        String carBasePrice = basePrice.getText();
        consoleLog.appendText("Amount: " + amount + "\n" + "CarType: " + type + "\n" + "CarFuelType: " + carFuelType + "\n"
                        + "Car mean price: " + carMeanPrice + "\n" + "Car price variance: " + carPriceVariance);
        if (!amount.isEmpty() && !type.isEmpty() && !carFuelType.isEmpty() && !carMeanPrice.isEmpty() && !carPriceVariance.isEmpty()) {
            carsToBeCreated.add(new String[]{amount, type, carFuelType, carMeanPrice, carPriceVariance, carBasePrice});
            populateTable();
            consoleLog.appendText("Car combinations to be created: " + carsToBeCreated.size() + "\n");
            dataBaseTableName = dealerShipName.getText();
        }
        amountOfCars.clear();
        meanPrice.clear();
        priceVariance.clear();
    }

    @FXML
    private void handleStartSimulation() {
        // Implement simulation logic here
        Trace.setTraceLevel(Trace.Level.INFO);
        carTable.getItems().clear(); // Clear the table view
        int arrivalMean = (int)arrivalMeanSlider.getValue();
        int arrivalVariance = (int) arrivalVarianceSlider.getValue();
        int financeMean = (int) financeMeanSlider.getValue();
        int financeVariance = (int) financeVarianceSlider.getValue();
        int testdriveMean = (int) testDriveMeanSlider.getValue();
        int testdriveVariance = (int) testDriveVarianceSlider.getValue();
        int closureMean = (int) closureMeanSlider.getValue();
        int closureVariance = (int) closureVarianceSlider.getValue();
        int simulationTime = 1440;
        simulationSpeed = 50;
        int arrivalServicePoint = arrivalServicePoints.getValue();
        int financeServicePoint = financeServicePoints.getValue();
        int testdriveServicePoint = 5;
        int closureServicePoint = closureServicePoints.getValue();
        int vanMean = 30000;
        int vanVariance = 5000;
        int suvMean = 40000;
        int suvVariance = 6000;
        int sedanMean = 30000;
        int sedanVariance = 4000;
        int sportMean = 60000;
        int sportVariance = 10000;
        int compactMean = 20000;
        int compactVariance = 3000;
        consoleLog.appendText("Simulation initialized with the following values:");
        consoleLog.appendText("\nArrival mean: " + arrivalMean + "\nArrival Variance: " + arrivalVariance);
        consoleLog.appendText("\nFinance mean: " + financeMean + "\nFinance Variance: " + financeVariance);
        consoleLog.appendText("\nTest drive mean: " + testdriveMean + "\nTest drive Variance: " + testdriveVariance);
        consoleLog.appendText("\nClosure mean: " + closureMean + "\nClosure Variance: " + closureVariance);
        consoleLog.appendText("\nArrival service points: " + arrivalServicePoint);
        consoleLog.appendText("\nFinance service points: " + financeServicePoint);
        consoleLog.appendText("\nTest drive service points: " + testdriveServicePoint);
        consoleLog.appendText("\nClosure service points: " + closureServicePoint + "\n");

        //createCars();
        //createCarsFromDb();
        simuController.creatTable(dataBaseTableName);
        simuController.addCarsToTable(dataBaseTableName, carsToBeCreated);

        simuController.initializeSimulation(arrivalMean, arrivalVariance, financeMean, financeVariance, testdriveMean,
                                            testdriveVariance, closureMean, closureVariance, simulationSpeed
                                            ,carsToBeCreated, arrivalServicePoint, financeServicePoint,
                                            testdriveServicePoint, closureServicePoint);

        simuController.setSimulationTime(simulationTime);
        simulationThread = new Thread(simuController);
        simulationThread.start();
        // Monitor simulation progress in a separate thread
        updateUI();
    }

    public void changeSimulationSpeed(){
        try {
            int multiplier = (int) simulationSpeedSlider.getValue();
            if (multiplier == 1 || multiplier <= 0) multiplier = 0;
            int newSimulationSpeed = simulationSpeed * multiplier;
            int actualSimulationSpeed = simuController.getMyEngine().getSimulationSpeed();
            if (newSimulationSpeed != actualSimulationSpeed) {
                actualSimulationSpeed = newSimulationSpeed;
                simuController.getMyEngine().setSimulationSpeed(actualSimulationSpeed);
                Platform.runLater(() -> consoleLog.appendText("Simulation speed: " + simuController.getMyEngine().getSimulationSpeed() + "\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> consoleLog.appendText("Error: " + e.getMessage() + "\n"));
            return; // Handle unexpected exceptions
        }
    }

    public void displaySimulationTime(){
        try {
            String newTime = String.valueOf((int) Clock.getInstance().getClock());
            Platform.runLater(() -> consoleLogLabel.setText("Current Simulation time: " + newTime + "\n"));
            Thread.sleep(500);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUI() {
        new Thread(() -> {
            int customersBefore = 0;
            int carsSoldBefore = 0;
            while (!simuController.isSimulationComplete()) {
                displaySimulationTime();
                changeSimulationSpeed();
                try {
                    int carsSold = simuController.getMyEngine().getCarDealerShop().getSoldCars().size();
                    if (carsSoldBefore != carsSold) {
                        carsSoldBefore = carsSold;
                        Platform.runLater(() -> consoleLog.appendText("Cars sold: " + carsSold + "\n"));
                    }
                    int customers = simuController.getMyEngine().getProcessedCustomer().size();
                    if (customersBefore != customers) {
                        customersBefore = customers;
                        Platform.runLater(() -> consoleLog.appendText("Customers processed: " + customers + "\n"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> consoleLog.appendText("Error: " + e.getMessage() + "\n"));
                    return; // Handle unexpected exceptions
                }
            }
            results();
        }).start();
    }

    public void createCars() {
        // Create a copy of carsToBeCreated to avoid concurrent modification
        List<String[]> carCopy = new ArrayList<>(carsToBeCreated);

        for (String[] car : carCopy) {
            String amount = car[0];
            String carType = car[1];
            String fuelType = car[2];
            String meanPrice = car[3];
            String priceVariance = car[4];

            // Add the car data to the list
            carsToBeCreated.add(new String[]{amount, carType, fuelType, meanPrice, priceVariance});
        }
    }

    public void createCarsFromDb(){
        ArrayList<String[]> cars;
        ArrayList<String> tableNames = simuController.getTableNames();
        tableName = tableNames.get(2);
        //tableName = carSets.getValue();
        cars = simuController.getCarsToBeCreated(tableName);

        for (String[] car : cars) {
            String amount = car[0];
            String carType = car[1];
            String fuelType = car[2];
            String meanPrice = car[3];
            String priceVariance = car[4];
            String basePrice = car[5];

            // Add the car data to the list
            carsToBeCreated.add(new String[]{amount, carType, fuelType, meanPrice, priceVariance, basePrice});
        }
    }


    public void getCarsFromDb() {
        // Clear previous data in the table
        carsToBeCreated.clear();

        // Get the selected table name from carType ComboBox
        String tableName = carSets.getValue(); // THIS WILL BE CHANGED LATER

        if (tableName != null) {
            // Fetch the new data based on the selected car type
            ArrayList<String[]> cars = simuController.getCarsToBeCreated(tableName);

            // Populate the carsToBeCreated list with the new data
            for (String[] car : cars) {
                String amount = car[0];
                String carType = car[1];
                String fuelType = car[2];
                String meanPrice = car[3];
                String priceVariance = car[4];
                String basePrice = car[5];

                // Add the car data to the list
                carsToBeCreated.add(new String[]{amount, carType, fuelType, meanPrice, priceVariance});
            }
        }
    }


    public void results() {
        Platform.runLater(() -> {
            // Clear previous logs
            //consoleLog.clear();

            consoleLog.setFont(new Font("Arial", 1)); // Set font to Arial with size 10
            consoleLog.setWrapText(false);

            // Append the simulation end time and customer count
            consoleLogLabel.setText("Simulation ended at: " + (int) Clock.getInstance().getClock());
            consoleLog.appendText("\nSimulation ended at: " + (int) Clock.getInstance().getClock());
            consoleLog.appendText("\nResults for: " + tableName);
            consoleLog.appendText("\nProcessed Customers: " + simuController.getMyEngine().getProcessedCustomer().size() + "\n");

            // Initialize containers for all car and fuel types
            Set<String> allCarTypes = new HashSet<>();
            Set<String> allFuelTypes = new HashSet<>();

            // Collect all unique car and fuel types
            for (Customer customer : simuController.getMyEngine().getProcessedCustomer()) {
                allCarTypes.add(customer.getPreferredCarType());
                allFuelTypes.add(customer.getPreferredFuelType());
            }

            // Print header and customer data
            consoleLog.appendText(String.format(
                    "%-10s %-15s %-15s %-15s %-20s %-15s %-10s %-12s %-18s %-22s %-20s\n",
                    "ID", "Arrival Time", "Removal Time", "Total time", "Preferred Car Type", "Fuel Type",
                    "Budget", "Credit Score", "Finance Accepted", "Happy with Test-drive", "Purchased a Car"
            ));

            double totalArrivalTime = 0, totalRemovalTime = 0, totalTime = 0, totalBudget = 0, totalCreditScore = 0;
            int totalCustomers = simuController.getMyEngine().getProcessedCustomer().size();
            int purchasedCount = 0;

            for (Customer customer : simuController.getMyEngine().getProcessedCustomer()) {
                totalArrivalTime += customer.getArrivalTime();
                totalRemovalTime += customer.getRemovalTime();
                totalTime += customer.getTotalTime();
                totalBudget += customer.getBudget();
                totalCreditScore += customer.getCreditScore();
                if (customer.isPurchased()) purchasedCount++;

                consoleLog.appendText(String.format(
                        "%-10d %-15.2f %-15.2f %-15.2f %-20s %-15s %-10.2f %-12d %-18b %-22b %-20b\n",
                        customer.getId(),
                        customer.getArrivalTime(),
                        customer.getRemovalTime(),
                        customer.getTotalTime(),
                        customer.getPreferredCarType(),
                        customer.getPreferredFuelType(),
                        customer.getBudget(),
                        customer.getCreditScore(),
                        customer.isFinanceAccepted(),
                        customer.happyWithTestdrive(),
                        customer.isPurchased()
                ));
            }

            // Print averages
            consoleLog.appendText(String.format(
                    "%-10s %-15.2f %-15.2f %-15.2f %-20s %-15s %-10.2f %-12.2f %-18s %-22s %-20.2f\n",
                    "AVG",
                    totalArrivalTime / totalCustomers,
                    totalRemovalTime / totalCustomers,
                    totalTime / totalCustomers,
                    "N/A",
                    "N/A",
                    totalBudget / totalCustomers,
                    totalCreditScore / totalCustomers,
                    "1.00",
                    "1.00",
                    purchasedCount / (double) totalCustomers
            ));

            // Initialize maps for sold counts
            Map<String, Integer> carTypeSoldCounts = new HashMap<>();
            Map<String, Integer> fuelTypeSoldCounts = new HashMap<>();
            Map<String, Integer> carFuelTypeSoldCounts = new HashMap<>();
            int totalSoldCars = 0;

            // Initialize counts to 0 for all car and fuel types
            for (String carType : allCarTypes) {
                carTypeSoldCounts.put(carType, 0);
            }
            for (String fuelType : allFuelTypes) {
                fuelTypeSoldCounts.put(fuelType, 0);
            }
            for (String carType : allCarTypes) {
                for (String fuelType : allFuelTypes) {
                    carFuelTypeSoldCounts.put(carType + " - " + fuelType, 0);
                }
            }

            // Count sold cars
            for (Customer customer : simuController.getMyEngine().getProcessedCustomer()) {
                if (customer.isPurchased()) {
                    totalSoldCars++;
                    String carType = customer.getPreferredCarType();
                    String fuelType = customer.getPreferredFuelType();

                    carTypeSoldCounts.put(carType, carTypeSoldCounts.get(carType) + 1);
                    fuelTypeSoldCounts.put(fuelType, fuelTypeSoldCounts.get(fuelType) + 1);
                    carFuelTypeSoldCounts.put(carType + " - " + fuelType, carFuelTypeSoldCounts.get(carType + " - " + fuelType) + 1);
                }
            }

            // Print car type preferences
            consoleLog.appendText("\nCar Type Preferences (All Customers):\n");
            for (String carType : allCarTypes) {
                long count = simuController.getMyEngine().getProcessedCustomer().stream()
                        .filter(customer -> customer.getPreferredCarType().equals(carType))
                        .count();
                consoleLog.appendText(carType + ": " + String.format("%.2f", (count / (double) totalCustomers) * 100) + "%\n");
            }

            // Print fuel type preferences
            consoleLog.appendText("\nFuel Type Preferences (All Customers):\n");
            for (String fuelType : allFuelTypes) {
                long count = simuController.getMyEngine().getProcessedCustomer().stream()
                        .filter(customer -> customer.getPreferredFuelType().equals(fuelType))
                        .count();
                consoleLog.appendText(fuelType + ": " + String.format("%.2f", (count / (double) totalCustomers) * 100) + "%\n");
            }

            // Print sold percentages by car type
            consoleLog.appendText("\nCar Type Sold Percentages:\n");
            for (String carType : allCarTypes) {
                int soldCount = carTypeSoldCounts.getOrDefault(carType, 0);
                consoleLog.appendText(carType + ": " + String.format("%.2f", (soldCount / (double) totalSoldCars) * 100) + "%\n");
            }

            // Print sold percentages by fuel type
            consoleLog.appendText("\nFuel Type Sold Percentages:\n");
            for (String fuelType : allFuelTypes) {
                int soldCount = fuelTypeSoldCounts.getOrDefault(fuelType, 0);
                consoleLog.appendText(fuelType + ": " + String.format("%.2f", (soldCount / (double) totalSoldCars) * 100) + "%\n");
            }

            // Print sold percentages by car-fuel combinations
            consoleLog.appendText("\nCar Type and Fuel Type Combination Sold Percentages:\n");
            for (String combination : carFuelTypeSoldCounts.keySet()) {
                int soldCount = carFuelTypeSoldCounts.get(combination);
                consoleLog.appendText(combination + ": " + String.format("%.2f", (soldCount / (double) totalSoldCars) * 100) + "%\n");
            }

            // Print remaining cars with prices
            consoleLog.appendText("\nCars left at the dealershop:\n");
            for (simu.model.Car car : simuController.getMyEngine().getCarDealerShop().getCarCollection()) {
                consoleLog.appendText(car.getCarType() + " (" + car.getFuelType() + ") - Base price at: $" + car.getBasePrice() + ") - Selling price at: $" + car.getMeanPrice() + "\n");
            }

            for (simu.model.Car car : simuController.getMyEngine().getCarDealerShop().getSoldCars()) {
                if (!simuController.getMyEngine().getCarDealerShop().getSoldCars().isEmpty()) {
                    consoleLog.appendText(car.getCarType() + " (" + car.getFuelType() + ") - Base price at: $" + car.getBasePrice() + ") - Sold at Price: $" + car.getMeanPrice() + "\n");
                }
            }

            // Print remaining cars and sold cars
            consoleLog.appendText("\nCars left at the dealershop: " + simuController.getMyEngine().getCarDealerShop().getCarCollection().size() + "\n");
            consoleLog.appendText("\nCars sold: " + totalSoldCars + "\n");
        });
    }


    public static class Car {
        private final String model;
        private final String type;
        private final double price;

        public Car(String model, String type, double price) {
            this.model = model;
            this.type = type;
            this.price = price;
        }

        public String getModel() { return model; }
        public String getType() { return type; }
        public double getPrice() { return price; }
    }
}