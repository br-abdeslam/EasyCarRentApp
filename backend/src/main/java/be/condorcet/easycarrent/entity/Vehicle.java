package be.condorcet.easycarrent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single rentable vehicle belonging to exactly one {@link VehicleCategory}.
 *
 * <p>The relationship to the category is required and lazily loaded through the
 * {@code category_id} foreign key.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(nullable = false, length = 60)
    private String brand;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear;

    @Column(length = 40)
    private String color;

    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @Column
    private Long mileage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private VehicleCategory category;

    protected Vehicle() {
        // Required no-argument constructor for JPA.
    }

    /**
     * Creates a vehicle that defaults to {@link VehicleStatus#AVAILABLE}.
     */
    public Vehicle(String registrationNumber,
                   String brand,
                   String model,
                   Integer manufacturingYear,
                   String color,
                   BigDecimal dailyPrice,
                   Long mileage,
                   VehicleCategory category) {
        this(registrationNumber, brand, model, manufacturingYear, color,
                dailyPrice, mileage, VehicleStatus.AVAILABLE, category);
    }

    public Vehicle(String registrationNumber,
                   String brand,
                   String model,
                   Integer manufacturingYear,
                   String color,
                   BigDecimal dailyPrice,
                   Long mileage,
                   VehicleStatus status,
                   VehicleCategory category) {
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.color = color;
        this.dailyPrice = dailyPrice;
        this.mileage = mileage;
        this.status = status != null ? status : VehicleStatus.AVAILABLE;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(Integer manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public Long getMileage() {
        return mileage;
    }

    public void setMileage(Long mileage) {
        this.mileage = mileage;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public void setCategory(VehicleCategory category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vehicle that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Vehicle{id=" + id
                + ", registrationNumber='" + registrationNumber + "'"
                + ", status=" + status + "}";
    }
}
