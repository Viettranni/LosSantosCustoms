package simu.model.cartypes; 

import simu.model.Car;

public class Sport extends Car {
    public Sport(String fuelType, double meanPrice, double priceVariance) {
        super("Sedan", fuelType, meanPrice, priceVariance);
    }
}